package com.hmdp.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 接收其他实例的登出广播，清掉本机 JWT 缓存。
 *
 * 这是"JWT 本地缓存"和"登出即时生效"能同时成立的唯一理由：
 * 本地缓存本身没法感知别的实例发生了什么，只能靠广播把结论推过来。
 * 本实例自己发的消息也会收到一次，重复 evict 无副作用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInvalidateListener implements MessageListener {

    private final JwtTokenCache jwtTokenCache;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String token = new String(message.getBody(), StandardCharsets.UTF_8);
        jwtTokenCache.evict(token);
        log.debug("[登出广播] 本机 JWT 缓存已清除");
    }
}
