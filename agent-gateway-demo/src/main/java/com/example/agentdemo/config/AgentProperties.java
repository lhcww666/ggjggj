package com.example.agentdemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private String toolKey;
    private long contextTtlSeconds = 1800;

    public String getToolKey() {
        return toolKey;
    }

    public void setToolKey(String toolKey) {
        this.toolKey = toolKey;
    }

    public long getContextTtlSeconds() {
        return contextTtlSeconds;
    }

    public void setContextTtlSeconds(long contextTtlSeconds) {
        this.contextTtlSeconds = contextTtlSeconds;
    }
}
