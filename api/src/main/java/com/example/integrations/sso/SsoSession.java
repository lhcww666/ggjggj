package com.example.integrations.sso;

public class SsoSession {

    private final String localUserId;
    private final SsoTarget target;
    private final long expiresAt;

    public SsoSession(String localUserId, SsoTarget target, long expiresAt) {
        this.localUserId = localUserId;
        this.target = target;
        this.expiresAt = expiresAt;
    }

    public String getLocalUserId() { return localUserId; }
    public SsoTarget getTarget() { return target; }
    public long getExpiresAt() { return expiresAt; }
    public boolean isExpired() { return expiresAt <= System.currentTimeMillis(); }
}
