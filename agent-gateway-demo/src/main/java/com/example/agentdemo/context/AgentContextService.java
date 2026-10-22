package com.example.agentdemo.context;

import com.example.agentdemo.config.AgentProperties;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 学习版用内存保存上下文。
 * 生产版应该换成 Redis，并设置 TTL、用户、组织、应用、租户等字段。
 */
@Service
public class AgentContextService {

    private final ConcurrentMap<String, AgentContext> contexts = new ConcurrentHashMap<>();
    private final AgentProperties properties;

    public AgentContextService(AgentProperties properties) {
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
        contexts.put(contextId, context);
        return context;
    }

    public AgentContext resolve(String contextId) {
        if (isBlank(contextId)) {
            return null;
        }
        AgentContext context = contexts.get(contextId);
        if (context == null || context.isExpired()) {
            contexts.remove(contextId);
            return null;
        }
        return context;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
