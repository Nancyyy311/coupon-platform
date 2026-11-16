package com.hmdp.job;

import com.hmdp.utils.JobLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单归档任务的单元测试。
 *
 * 这个任务平时最不容易出问题，一旦出问题又最难发现——它在凌晨 3 点跑，
 * 失败了没人在线，表现是"用户翻自己的历史订单少了几天"，而不是报错。
 * 所以要盯的不是"能不能搬"，而是三个只在多实例 / 表结构变化时才暴露的点：
 *
 *   1. 没抢到锁必须立刻退出，不能两个实例同时搬同一批（后者必然主键冲突）
 *   2. INSERT 必须写显式列名 + ON DUPLICATE KEY UPDATE，不能用 SELECT *
 *      （SELECT * 依赖两表列顺序一致；裸 INSERT 遇到已归档的行会抛异常中断整次归档）
 *   3. 必须有批次上限，否则"删不掉导致 SELECT 一直返回同一批"时会无限循环
 */
@ExtendWith(MockitoExtension.class)
class OrderArchiveJobTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private JobLock jobLock;

    @InjectMocks
    private OrderArchiveJob job;

    /** 让"查出一批 id"这件事在第二次调用时结束，避免测试里真的循环 */
    private void givenOneBatchThenEmpty(Long... ids) {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(ids))
                .thenReturn(Collections.emptyList());
        when(jdbcTemplate.update(anyString())).thenReturn(ids.length);
    }

    @Test
    @DisplayName("抢不到锁直接返回，连库都不查——这是多实例下不重复归档的前提")
    void skipsEntirelyWhenLockBusy() {
        when(jobLock.tryAcquire(anyString(), anyLong())).thenReturn(null);

        job.archive();

        verify(jdbcTemplate, never()).queryForList(anyString(), eq(Long.class), any(LocalDateTime.class));
        verify(jdbcTemplate, never()).update(anyString());
        // 没拿到锁，fixture 里也不该去释放（否则会删掉别人的锁）
        verify(jobLock, never()).release(anyString(), any());
    }

    @Test
    @DisplayName("INSERT 使用显式列名，不出现 SELECT *（防主表加列后凌晨三点才报列数不匹配）")
    void insertUsesExplicitColumns() {
        when(jobLock.tryAcquire(anyString(), anyLong())).thenReturn("token");
        givenOneBatchThenEmpty(1L, 2L);

        job.archive();

        String insert = captureFirstUpdate();
        assertTrue(insert.startsWith("INSERT INTO tb_voucher_order_history (id, user_id, voucher_id"),
                "必须显式列出列名，实际：" + insert);
        assertFalse(insert.contains("SELECT *"), "不能依赖两表列顺序一致，实际：" + insert);
    }

    @Test
    @DisplayName("INSERT 带 ON DUPLICATE KEY UPDATE：某条已归档时跳过，不中断整批")
    void insertToleratesDuplicateRows() {
        when(jobLock.tryAcquire(anyString(), anyLong())).thenReturn("token");
        givenOneBatchThenEmpty(1L, 2L);

        job.archive();

        String insert = captureFirstUpdate();
        assertTrue(insert.contains("ON DUPLICATE KEY UPDATE"),
                "缺了它，一条主键冲突就会让整次归档 break，实际：" + insert);
        assertFalse(insert.contains("INSERT IGNORE"),
                "INSERT IGNORE 会把字段截断这类错误一起吞掉，只该忽略主键冲突");
    }

    @Test
    @DisplayName("ON DUPLICATE 的列必须全限定：写成 id = id 会报 ambiguous，且只在凌晨执行时才暴露")
    void duplicateKeyUpdateUsesQualifiedColumn() {
        when(jobLock.tryAcquire(anyString(), anyLong())).thenReturn("token");
        givenOneBatchThenEmpty(1L, 2L);

        job.archive();

        String insert = captureFirstUpdate();
        assertTrue(insert.contains("ON DUPLICATE KEY UPDATE tb_voucher_order_history.id = tb_voucher_order_history.id"),
                "INSERT ... SELECT 下不加表名会报 Column 'id' is ambiguous（已在 MySQL 实测），实际：" + insert);
    }

    @Test
    @DisplayName("跑完释放锁（finally），异常时也要释放")
    void releasesLockEvenOnFailure() {
        when(jobLock.tryAcquire(anyString(), anyLong())).thenReturn("token");
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(LocalDateTime.class)))
                .thenThrow(new IllegalStateException("table not found"));

        assertDoesNotThrow(() -> job.archive(), "异常不能抛给调度线程，否则这一轮直接崩掉");

        verify(jobLock).release("order-archive", "token");
    }

    @Test
    @DisplayName("批次失败只中止本次归档，不向上抛异常")
    void batchFailureStopsButDoesNotThrow() {
        when(jobLock.tryAcquire(anyString(), anyLong())).thenReturn("token");
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(1L));
        when(jdbcTemplate.update(anyString())).thenThrow(new RuntimeException("Duplicate entry"));

        assertDoesNotThrow(() -> job.archive());

        verify(jobLock).release("order-archive", "token");
    }

    @Test
    @DisplayName("有批次上限：SELECT 一直有结果时最多跑 100 批就停，不会无限循环")
    void stopsAtMaxBatches() {
        when(jobLock.tryAcquire(anyString(), anyLong())).thenReturn("token");
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(1L));
        when(jdbcTemplate.update(anyString())).thenReturn(1);

        job.archive();

        verify(jdbcTemplate, times(100))
                .queryForList(anyString(), eq(Long.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("没有可归档订单时不执行任何写操作")
    void doesNothingWhenNoOrders() {
        when(jobLock.tryAcquire(anyString(), anyLong())).thenReturn("token");
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        job.archive();

        verify(jdbcTemplate, never()).update(anyString());
        verify(jobLock).release("order-archive", "token");
    }

    @Test
    @DisplayName("Redis 降级（空串）时照常执行归档：归档只碰 MySQL，不该被 Redis 拖停")
    void proceedsWhenLockDegraded() {
        when(jobLock.tryAcquire(anyString(), anyLong())).thenReturn("");
        givenOneBatchThenEmpty(1L);

        job.archive();

        verify(jdbcTemplate, atLeastOnce()).queryForList(anyString(), eq(Long.class), any(LocalDateTime.class));
        // 降级那次没真加锁，release 内部会判空跳过
        verify(jobLock).release("order-archive", "");
    }

    private String captureFirstUpdate() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).update(captor.capture());
        return captor.getAllValues().get(0);
    }
}
