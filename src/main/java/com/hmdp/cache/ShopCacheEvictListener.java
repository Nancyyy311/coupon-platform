package com.hmdp.cache;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 商户变更后要做的两件事：清缓存、同步地理索引（都在提交之后）。
 *
 * 三个关键点：
 *   1. AFTER_COMMIT  只在事务提交成功后执行，回滚不触发。
 *      这样其他节点收到广播时，数据库里已经是新值，回源只会读到新值。
 *   2. fallbackExecution = true  如果事件不是从事务里发出的（比如后台脚本直接 save），
 *      也照样执行。否则"没有事务"会变成"永远不删缓存"，比原来的 bug 更隐蔽。
 *   3. 两件事分别 try-catch，不能共用一个 catch。
 *      事务已经提交，这里再抛异常只会让接口返回 500，数据其实是成功的。
 *      更重要的区别：**缓存不一致会自愈（TTL 兜底），GEO 索引不一致不会**——
 *      它没有过期时间，错了就一直错。所以缓存失败也必须继续同步 GEO。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShopCacheEvictListener {

    private final ShopCacheInvalidator invalidator;
    private final IShopService shopService;
    private final ShopGeoIndexer geoIndexer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onShopChanged(ShopChangedEvent event) {
        Long shopId = event.getShopId();
        if (shopId == null) {
            return;
        }

        try {
            // 第一次：立刻删 Redis + 删本机 L1 + 广播，其他实例同步清 L1
            invalidator.evict(shopId);
            // 第二次：延迟删，兜住"读线程在第一次删除之后才把旧值写回缓存"的窗口
            invalidator.evictLater(shopId, ShopCacheInvalidator.SECOND_EVICT_DELAY_MS);
        } catch (Exception e) {
            log.error("[缓存失效] shopId={} 处理失败，等待 TTL 兜底", shopId, e);
        }

        try {
            // 提交后再查，拿到的是最新值；变更前的值由事件带过来
            Shop latest = shopService.getById(shopId);
            geoIndexer.sync(event, latest);
        } catch (Exception e) {
            log.error("[GEO同步] shopId={} 处理失败，附近商户会缺这家店或位置偏差", shopId, e);
        }
    }
}
