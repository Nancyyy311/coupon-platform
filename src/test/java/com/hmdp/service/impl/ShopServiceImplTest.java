package com.hmdp.service.impl;

import com.hmdp.cache.ShopChangedEvent;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.exception.ParamException;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.utils.CacheClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.geo.Circle;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 商户写路径的单元测试。
 *
 * 核心只有一条：**写操作不能自己去删缓存，只能发事件**。
 * 因为删缓存必须发生在事务提交之后，而在方法体里写的时候事务还没提交。
 * 上一版就是在这里直接调 invalidator，导致其他节点收到广播后回源读到未提交的旧值。
 */
@ExtendWith(MockitoExtension.class)
class ShopServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private CacheClient cacheClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ShopMapper shopMapper;
    private ShopServiceImpl shopService;

    @BeforeEach
    void setUp() {
        shopService = spy(new ShopServiceImpl(stringRedisTemplate, cacheClient, eventPublisher));
        shopMapper = mock(ShopMapper.class);
        ReflectionTestUtils.setField(shopService, "baseMapper", shopMapper);
    }

    @Test
    @DisplayName("更新店铺：只发事件，不在事务里直接删缓存；事件带变更前的分类与坐标")
    void updatePublishesEventInsteadOfEvicting() {
        Shop before = new Shop();
        before.setId(1L);
        before.setTypeId(1L);
        before.setX(120.149192);
        before.setY(30.316078);
        doReturn(before).when(shopService).getById(1L);

        Shop shop = new Shop();
        shop.setId(1L);
        shop.setName("新的店名");
        doReturn(true).when(shopService).updateById(any(Shop.class));

        Result result = shopService.update(shop);

        assertTrue(result.getSuccess());
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ShopChangedEvent event = (ShopChangedEvent) captor.getValue();
        assertEquals(1L, event.getShopId());
        // 旧分类/旧坐标必须在事务内捎出来：提交后查库只能查到新值，GEO 迁移就无从判断
        assertEquals(1L, event.getOldTypeId());
        assertEquals(120.149192, event.getOldX());
        assertEquals(30.316078, event.getOldY());

        // 关键：这里绝不能直接操作缓存，否则就是在事务提交前删缓存
        verifyNoInteractions(cacheClient);
    }

    @Test
    @DisplayName("更新不存在的店铺：直接拒绝，不写库也不发事件")
    void updateMissingShopRejected() {
        doReturn(null).when(shopService).getById(404L);

        Shop shop = new Shop();
        shop.setId(404L);
        shop.setName("不存在的店");

        Result result = shopService.update(shop);

        assertFalse(result.getSuccess());
        assertEquals("店铺不存在！", result.getErrorMsg());
        // 连库都不该写：id 不存在时 updateById 只会白跑一趟
        verify(shopService, never()).updateById(any(Shop.class));
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("更新时没有 id：直接拒绝，不发事件也不写库")
    void updateWithoutIdRejected() {
        Shop shop = new Shop();

        Result result = shopService.update(shop);

        assertFalse(result.getSuccess());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
        verifyNoInteractions(cacheClient);
    }

    @Test
    @DisplayName("新增店铺成功：也要发事件（清掉可能残留的空值缓存）")
    void savePublishesEvent() {
        when(shopMapper.insert(any(Shop.class))).thenAnswer(invocation -> {
            Shop saved = invocation.getArgument(0);
            saved.setId(100L); // 模拟自增主键回填
            return 1;
        });
        Shop shop = validShop();
        shop.setName("新开的店");

        boolean saved = shopService.save(shop);

        assertTrue(saved);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(100L, ((ShopChangedEvent) captor.getValue()).getShopId());
    }

    @Test
    @DisplayName("新增失败：不发事件，避免做无意义的缓存清理")
    void saveFailureDoesNotPublish() {
        when(shopMapper.insert(any(Shop.class))).thenReturn(0);

        boolean saved = shopService.save(validShop());

        assertFalse(saved);
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    /** 一个能通过写入口校验的店铺，各字段都落在 tb_shop 的列宽/符号范围内 */
    private static Shop validShop() {
        Shop shop = new Shop();
        shop.setName("测试店");
        shop.setTypeId(1L);
        shop.setImages("test.jpg");
        shop.setAddress("测试路 1 号");
        shop.setX(120.149192);
        shop.setY(30.316078);
        shop.setSold(0);
        shop.setComments(0);
        shop.setScore(40);
        return shop;
    }

    // ============================================================
    // 写入口的入参校验。
    // 这一组对应的都是实测出来的"能打出 500 服务器异常"的入参：
    //   店名 200 字  -> Data too long for column 'name'
    //   坐标 -1      -> Out of range value for column 'x'（列是 double unsigned）
    //   销量 -5      -> Out of range value for column 'sold'
    // 校验放在 service 里，所以这几条都直接打 service，不经过 controller。
    // ============================================================

    @Test
    @DisplayName("新增店铺：店名超 128 字直接拒绝，不写库也不发事件")
    void saveRejectsTooLongName() {
        Shop shop = validShop();
        shop.setName(repeat('长', 129));

        ParamException e = assertThrows(ParamException.class, () -> shopService.save(shop));

        assertEquals("店铺名称最多 128 个字符", e.getMessage());
        verify(shopMapper, never()).insert(any(Shop.class));
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("新增店铺：坐标为负直接拒绝（列是 unsigned，负值到 MySQL 才炸就晚了）")
    void saveRejectsNegativeCoordinate() {
        Shop shop = validShop();
        shop.setX(-1.0);

        ParamException e = assertThrows(ParamException.class, () -> shopService.save(shop));

        // 注意这里和"越界"是两种文案：-1 经度在几何上完全合法（西半球），
        // 只是 tb_shop.x 是 double unsigned 存不进去。合成一句话会让人不知道该改什么。
        assertEquals("坐标不能为负数", e.getMessage());
        verify(shopMapper, never()).insert(any(Shop.class));
    }

    @Test
    @DisplayName("写入口坐标校验：几何不合法与存不进去给的是不同文案")
    void pointWriteErrorDistinguishesTwoKinds() {
        assertNull(ShopServiceImpl.pointWriteError(120.149192, 30.316078));
        // 越界/NaN -> Redis 不认
        assertEquals("坐标不合法", ShopServiceImpl.pointWriteError(999.0, 999.0));
        assertEquals("坐标不合法", ShopServiceImpl.pointWriteError(Double.NaN, 30.0));
        // 几何合法但列无符号 -> MySQL 不认
        assertEquals("坐标不能为负数", ShopServiceImpl.pointWriteError(-120.0, 30.0));
    }

    @Test
    @DisplayName("新增店铺：销量为负直接拒绝")
    void saveRejectsNegativeSold() {
        Shop shop = validShop();
        shop.setSold(-5);

        ParamException e = assertThrows(ParamException.class, () -> shopService.save(shop));

        assertEquals("销量不能为负数", e.getMessage());
        verify(shopMapper, never()).insert(any(Shop.class));
    }

    @Test
    @DisplayName("新增店铺：缺分类/地址时给的是字段名，不是数据库报错")
    void saveRejectsMissingRequiredField() {
        Shop shop = validShop();
        shop.setAddress(null);

        ParamException e = assertThrows(ParamException.class, () -> shopService.save(shop));

        assertEquals("店铺地址不能为空", e.getMessage());
    }

    @Test
    @DisplayName("更新店铺：只改名字时不校验坐标（用库里的值补齐后再判断）")
    void updateNameOnlyDoesNotTripCoordinateCheck() {
        Shop before = new Shop();
        before.setId(1L);
        before.setTypeId(1L);
        before.setX(120.149192);
        before.setY(30.316078);
        doReturn(before).when(shopService).getById(1L);
        doReturn(true).when(shopService).updateById(any(Shop.class));

        Shop shop = new Shop();
        shop.setId(1L);
        shop.setName("改个名字");

        Result result = shopService.update(shop);

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("更新店铺：把坐标改成越界值直接拒绝，不发事件")
    void updateRejectsInvalidCoordinate() {
        Shop before = new Shop();
        before.setId(1L);
        before.setX(120.149192);
        before.setY(30.316078);
        doReturn(before).when(shopService).getById(1L);

        Shop shop = new Shop();
        shop.setId(1L);
        shop.setX(999.0);

        ParamException e = assertThrows(ParamException.class, () -> shopService.update(shop));

        assertEquals("坐标不合法", e.getMessage());
        verify(shopService, never()).updateById(any(Shop.class));
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    private static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    // ============================================================
    // 附近商户的入参边界。
    // 这一组对应的是实测出来的两个"匿名就能打出服务器异常"的口子：
    //   ?current=-3  -> GEO 分支算出负下标 -> IndexOutOfBoundsException
    //   ?x=999&y=999 -> Redis 报 invalid longitude,latitude pair
    // ============================================================

    @Test
    @DisplayName("页码归一化：null / 0 / 负数都按第 1 页，正常值原样返回")
    void normalizePageClampsInvalidValues() {
        assertEquals(1, ShopServiceImpl.normalizePage(null));
        assertEquals(1, ShopServiceImpl.normalizePage(0));
        assertEquals(1, ShopServiceImpl.normalizePage(-3));
        assertEquals(2, ShopServiceImpl.normalizePage(2));
        assertEquals(100, ShopServiceImpl.normalizePage(100));
    }

    @Test
    @DisplayName("坐标校验：越界、NaN、Infinity 全部拒绝，正常坐标放行")
    void geoPointValidation() {
        // 正常：杭州西湖
        assertTrue(ShopServiceImpl.isUsableGeoPoint(120.149192, 30.316078));
        // 边界值本身是合法的
        assertTrue(ShopServiceImpl.isUsableGeoPoint(180.0, 85.05112878));
        assertTrue(ShopServiceImpl.isUsableGeoPoint(-180.0, -85.05112878));

        // 越界：Redis 会直接报 invalid longitude,latitude pair
        assertFalse(ShopServiceImpl.isUsableGeoPoint(999.0, 999.0));
        assertFalse(ShopServiceImpl.isUsableGeoPoint(-999.0, 30.0));
        // 地理上合法但超出 Web Mercator 范围（纬度 90 > 85.05112878）
        assertFalse(ShopServiceImpl.isUsableGeoPoint(120.0, 90.0));

        // NaN 与任何数比较都是 false，范围判断挡不住它，必须显式判
        assertFalse(ShopServiceImpl.isUsableGeoPoint(Double.NaN, Double.NaN));
        assertFalse(ShopServiceImpl.isUsableGeoPoint(Double.POSITIVE_INFINITY, 30.0));
        assertFalse(ShopServiceImpl.isUsableGeoPoint(null, 30.0));
    }

    @Test
    @DisplayName("附近商户：坐标不合法时直接拒绝，一个 Redis 命令都不发")
    void queryShopByTypeRejectsInvalidPoint() {
        Result result = shopService.queryShopByType(1, 1, 999.0, 999.0);

        assertFalse(result.getSuccess());
        assertEquals("坐标不合法", result.getErrorMsg());
        // 关键：必须在进 Redis 之前拦下，否则异常会被兜成"服务器异常"
        verifyNoInteractions(stringRedisTemplate);
    }

    /**
     * 超大页码曾经是第三个能打出 500 的口子。
     *
     * current 是 int，`current * 每页条数` 在 int 里会溢出。比如 429496730 * 5
     * 溢出成 -2147483646，传给 Redis 就是 `LIMIT 0 -2147483646`，
     * Redis 直接报 "COUNT must be > 0"。
     * 这条测试盯的就是"传给 Redis 的 limit 必须还是正数"。
     */
    @Test
    @DisplayName("附近商户：超大页码不会溢出成负数，limit 仍是正数")
    @SuppressWarnings("unchecked")
    void queryShopByTypeHugePageDoesNotOverflow() {
        GeoOperations<String, String> geoOps = mock(GeoOperations.class);
        when(stringRedisTemplate.opsForGeo()).thenReturn(geoOps);
        when(geoOps.radius(anyString(), any(Circle.class), any(GeoRadiusCommandArgs.class)))
                .thenReturn(null);

        // 429496730 * 5 = 2147483650，int 下会溢出为负数
        Result result = shopService.queryShopByType(1, 429496730, 120.149192, 30.316078);

        assertTrue(result.getSuccess(), "超大页码应返回空列表，而不是抛异常");
        ArgumentCaptor<GeoRadiusCommandArgs> captor = ArgumentCaptor.forClass(GeoRadiusCommandArgs.class);
        verify(geoOps).radius(anyString(), any(Circle.class), captor.capture());
        assertTrue(captor.getValue().getLimit() > 0, "传给 Redis 的 count 必须 > 0");
    }
}
