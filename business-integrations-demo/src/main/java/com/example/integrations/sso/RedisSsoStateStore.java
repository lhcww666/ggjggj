package com.example.integrations.sso;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

@Component
@ConditionalOnProperty(name = "demo.sso-state", havingValue = "redis")
public class RedisSsoStateStore implements SsoStateStore {

    private static final String NONCE_PREFIX = "demo:sso:nonce:";
    private static final String CODE_PREFIX = "demo:sso:code:";

    private final StringRedisTemplate redis;

    public RedisSsoStateStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean markNonce(String nonce, long ttlSeconds) {
        Boolean created = redis.opsForValue().setIfAbsent(
                NONCE_PREFIX + nonce, "1", Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(created);
    }

    @Override
    public void saveCode(String code, SsoSession session, long ttlSeconds) {
        String payload = session.getLocalUserId() + "|" + session.getTarget().getType() + "|"
                + session.getTarget().getPath() + "|" + session.getTarget().getId() + "|"
                + session.getExpiresAt();
        redis.opsForValue().set(CODE_PREFIX + code, payload, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public SsoSession takeCode(String code) {
        if (code == null) {
            return null;
        }
        String key = CODE_PREFIX + code;
        String payload = redis.opsForValue().get(key);
        redis.delete(key);
        if (payload == null) {
            return null;
        }
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 5) {
            return null;
        }
        SsoTarget target = new SsoTarget(parts[1], parts[2], parts[3]);
        return new SsoSession(parts[0], target, Long.parseLong(parts[4]));
    }
}
