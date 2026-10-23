package com.example.agentdemo.context;

import com.example.agentdemo.config.AgentProperties;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AgentContextService {

    private final AgentContextStore contextStore;
    private final AgentProperties properties;

    public AgentContextService(AgentContextStore contextStore, AgentProperties properties) {
        this.contextStore = contextStore;
        this.properties = properties;
    }

    public AgentContext create(String userId, String agentCode) {
        String contextId = UUID.randomUUID().toString().replace("-", "");
        long expiresAt = System.currentTimeMillis() + properties.getContextTtlSeconds() * 1000L;
        AgentContext context = new AgentContext(
                contextId,
                isBlank(userId) ? "demo-user" : userId,
                isBlank(agentCode) ? "default-agent" : agentCode,
                expiresAt
        );
        contextStore.deleteExpired(System.currentTimeMillis());
        contextStore.save(context);
        return context;
    }

    public AgentContext resolve(String contextId) {
        if (isBlank(contextId)) {
            return null;
        }
        AgentContext context = contextStore.find(contextId);
        if (context == null || context.isExpired()) {
            contextStore.delete(contextId);
            return null;
        }
        return context;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
