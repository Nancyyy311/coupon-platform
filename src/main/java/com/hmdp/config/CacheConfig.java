package com.hmdp.config;

import com.hmdp.cache.CacheInvalidateListener;
import com.hmdp.cache.JwtInvalidateListener;
import com.hmdp.cache.JwtTokenCache;
import com.hmdp.cache.ShopCacheInvalidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis 发布订阅配置：用于 L1 缓存跨实例失效。
 * 订阅端每个实例一个容器，收到广播后清自己的本地缓存。
 */
@Configuration
public class CacheConfig {

    @Bean
    public RedisMessageListenerContainer cacheInvalidateContainer(RedisConnectionFactory factory,
                                                                 CacheInvalidateListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(listener, new ChannelTopic(ShopCacheInvalidator.CHANNEL));
        return container;
    }

    /**
     * 登出广播：JWT 本地缓存跨实例失效。
     * 与店铺缓存共用一套思路，但独立频道，互不影响。
     */
    @Bean
    public RedisMessageListenerContainer jwtInvalidateContainer(RedisConnectionFactory factory,
                                                                JwtInvalidateListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(listener, new ChannelTopic(JwtTokenCache.CHANNEL));
        return container;
    }
}
