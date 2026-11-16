package com.hmdp.cache;

import com.hmdp.entity.Shop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * GEO 写路径的单元测试。
 *
 * 重点不是"调没调 GEOADD"，而是三个容易写错的分支：
 *   1) 只改名字 → 不许碰 Redis（否则每次改营业时间都白写一次索引）
 *   2) 换分类   → 必须先从旧 key 移走，否则旧分类的"附近"里永远留着这家店
 *   3) 坐标缺失 → 跳过写入，但不能抛异常（事务已提交，抛了也回不去）
 */
@ExtendWith(MockitoExtension.class)
class ShopGeoIndexerTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private GeoOperations<String, String> geoOperations;

    @InjectMocks
    private ShopGeoIndexer indexer;

    private void stubGeo() {
        lenient().when(stringRedisTemplate.opsForGeo()).thenReturn(geoOperations);
    }

    private Shop shop(long id, long typeId, double x, double y) {
        Shop s = new Shop();
        s.setId(id);
        s.setTypeId(typeId);
        s.setX(x);
        s.setY(y);
        return s;
    }

    @Test
    @DisplayName("新增店铺：写入对应分类的 GEO")
    void insertWritesGeo() {
        stubGeo();
        Shop after = shop(15L, 1L, 120.1493, 30.3161);

        indexer.sync(new ShopChangedEvent(15L), after);

        verify(geoOperations).add(eq("shop:geo:1"),
                argThat((Point p) -> p.getX() == 120.1493 && p.getY() == 30.3161),
                eq("15"));
        verify(geoOperations, never()).remove(anyString(), any());
    }

    @Test
    @DisplayName("只改名字：坐标和分类都没变，不碰 Redis")
    void renameDoesNotTouchGeo() {
        stubGeo();
        Shop after = shop(1L, 1L, 120.149192, 30.316078);

        indexer.sync(new ShopChangedEvent(1L, 1L, 120.149192, 30.316078), after);

        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("搬位置：同分类内更新坐标，不需要先删")
    void moveUpdatesCoordinate() {
        stubGeo();
        Shop after = shop(1L, 1L, 121.5, 31.4);

        indexer.sync(new ShopChangedEvent(1L, 1L, 120.149192, 30.316078), after);

        verify(geoOperations).add(eq("shop:geo:1"),
                argThat((Point p) -> p.getX() == 121.5), eq("1"));
        verify(geoOperations, never()).remove(anyString(), any());
    }

    @Test
    @DisplayName("换分类：先从旧 key 移除，再写进新 key")
    void typeChangeMigratesKey() {
        stubGeo();
        Shop after = shop(1L, 2L, 120.149192, 30.316078);

        indexer.sync(new ShopChangedEvent(1L, 1L, 120.149192, 30.316078), after);

        verify(geoOperations).remove("shop:geo:1", "1");
        verify(geoOperations).add(eq("shop:geo:2"), any(Point.class), eq("1"));
    }

    @Test
    @DisplayName("店铺被删：只从旧分类移除，不写新索引")
    void deleteRemovesOnly() {
        stubGeo();

        indexer.sync(new ShopChangedEvent(1L, 1L, 120.1, 30.2), null);

        verify(geoOperations).remove("shop:geo:1", "1");
        verify(geoOperations, never()).add(anyString(), any(Point.class), anyString());
    }

    @Test
    @DisplayName("坐标缺失：跳过写入，不抛异常（事务已提交，抛了也回不去）")
    void missingCoordinateSkips() {
        stubGeo();
        Shop after = shop(15L, 1L, 0, 0);
        after.setX(null);

        indexer.sync(new ShopChangedEvent(15L), after);

        verify(geoOperations, never()).add(anyString(), any(Point.class), anyString());
    }
}
