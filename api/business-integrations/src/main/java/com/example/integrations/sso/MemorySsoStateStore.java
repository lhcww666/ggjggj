package com.example.integrations.sso;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@ConditionalOnProperty(name = "demo.sso-state", havingValue = "memory", matchIfMissing = true)
public class MemorySsoStateStore implements SsoStateStore {

    private final ConcurrentMap<String, Long> nonces = new ConcurrentHashMap<String, Long>();
    private final ConcurrentMap<String, SsoSession> codes = new ConcurrentHashMap<String, SsoSession>();

    @Override
    public boolean markNonce(String nonce, long ttlSeconds) {
        long now = System.currentTimeMillis();
        Long existing = nonces.get(nonce);
        if (existing != null && existing > now) {
            return false;
        }
        nonces.put(nonce, now + ttlSeconds * 1000L);
        return true;
    }

    @Override
    public void saveCode(String code, SsoSession session, long ttlSeconds) {
        codes.put(code, session);
    }

    @Override
    public SsoSession takeCode(String code) {
        return code == null ? null : codes.remove(code);
    }
}
