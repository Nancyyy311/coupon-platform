package com.hmdp.cache;

import com.hmdp.utils.CacheClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

/**
 * 接收其他实例的缓存失效广播，清掉本机 L1。
 * 本实例自己发出的消息也会收到一次，重复 invalidate 无副作用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidateListener implements MessageListener {

    private final CacheClient cacheClient;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String shopId = new String(message.getBody(), StandardCharsets.UTF_8);
        cacheClient.invalidateLocal(CACHE_SHOP_KEY + shopId);
        log.debug("[缓存失效广播] 本机 L1 已清除 shopId={}", shopId);
    }
}
