package com.example.agentdemo.context;

public class AgentContext {

    private final String contextId;
    private final String userId;
    private final String agentCode;
    private final long expiresAt;

    public AgentContext(String contextId, String userId, String agentCode, long expiresAt) {
        this.contextId = contextId;
        this.userId = userId;
        this.agentCode = agentCode;
        this.expiresAt = expiresAt;
    }

    public String getContextId() {
        return contextId;
    }

    public String getUserId() {
        return userId;
    }

    public String getAgentCode() {
        return agentCode;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return expiresAt <= System.currentTimeMillis();
    }
}
