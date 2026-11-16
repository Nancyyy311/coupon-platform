package com.hmdp.cache;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 缓存失效 + GEO 同步监听器的单元测试。
 *
 * 这里要盯死的不是"删没删"，而是三件事：
 *   1) 必须是"立即失效 + 延迟二次失效"两步，少一步就留下"读线程写回旧值"的窗口
 *   2) 失效过程中的异常不能往外抛——事务已经提交了，这时候报错给用户看只会误导
 *   3) 缓存失败不能连带 GEO 一起跳过——缓存有 TTL 会自愈，GEO 索引不会
 */
@ExtendWith(MockitoExtension.class)
class ShopCacheEvictListenerTest {

    @Mock
    private ShopCacheInvalidator invalidator;

    @Mock
    private IShopService shopService;

    @Mock
    private ShopGeoIndexer geoIndexer;

    @InjectMocks
    private ShopCacheEvictListener listener;

    @Test
    @DisplayName("商户变更：先立即失效，再按约定延迟二次失效，最后同步 GEO")
    void evictThenEvictLaterThenGeo() {
        Shop latest = new Shop();
        latest.setId(9L);
        when(shopService.getById(9L)).thenReturn(latest);

        listener.onShopChanged(new ShopChangedEvent(9L, 1L, 120.1, 30.2));

        InOrder order = inOrder(invalidator);
        order.verify(invalidator).evict(9L);
        order.verify(invalidator).evictLater(9L, ShopCacheInvalidator.SECOND_EVICT_DELAY_MS);
        // 先删缓存再动索引：反过来的话，用户会在"附近已更新、详情还是 30 分钟旧缓存"的
        // 窗口里看到自相矛盾的数据
        verify(geoIndexer).sync(org.mockito.ArgumentMatchers.any(ShopChangedEvent.class),
                org.mockito.ArgumentMatchers.eq(latest));
    }

    @Test
    @DisplayName("shopId 为空：不碰缓存也不碰索引，不能抛 NPE")
    void ignoreNullShopId() {
        listener.onShopChanged(new ShopChangedEvent(null));

        verifyNoInteractions(invalidator, shopService, geoIndexer);
    }

    @Test
    @DisplayName("失效抛异常：不允许把已提交的事务报成失败，但 GEO 仍要同步")
    void swallowEvictExceptionButStillSyncGeo() {
        doThrow(new RuntimeException("redis down")).when(invalidator).evict(anyLong());

        assertDoesNotThrow(() -> listener.onShopChanged(new ShopChangedEvent(9L)));

        // 第一次都没成功，就不该再排一次延迟删除（排了也只会再抛一次）
        verify(invalidator, never()).evictLater(anyLong(), anyLong());
        // 缓存有 TTL 兜底，GEO 没有；所以缓存失败不能连带跳过索引同步
        verify(geoIndexer).sync(org.mockito.ArgumentMatchers.any(ShopChangedEvent.class),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("GEO 同步抛异常：同样不能往外抛，数据已经提交成功")
    void swallowGeoException() {
        doThrow(new RuntimeException("geo down")).when(geoIndexer)
                .sync(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        assertDoesNotThrow(() -> listener.onShopChanged(new ShopChangedEvent(9L)));
    }
}
