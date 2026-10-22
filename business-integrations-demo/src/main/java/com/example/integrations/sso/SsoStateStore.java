package com.example.integrations.sso;

public interface SsoStateStore {

    boolean markNonce(String nonce, long ttlSeconds);

    void saveCode(String code, SsoSession session, long ttlSeconds);

    SsoSession takeCode(String code);
}
