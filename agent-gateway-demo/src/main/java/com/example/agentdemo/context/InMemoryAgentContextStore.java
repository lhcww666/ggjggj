package com.example.agentdemo.context;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@ConditionalOnProperty(prefix = "agent", name = "context-store", havingValue = "memory", matchIfMissing = true)
public class InMemoryAgentContextStore implements AgentContextStore {

    private final ConcurrentMap<String, AgentContext> contexts = new ConcurrentHashMap<String, AgentContext>();

    @Override
    public void save(AgentContext context) {
        contexts.put(context.getContextId(), context);
    }

    @Override
    public AgentContext find(String contextId) {
        return contexts.get(contextId);
    }

    @Override
    public void delete(String contextId) {
        contexts.remove(contextId);
    }

    @Override
    public void deleteExpired(long now) {
        for (AgentContext context : contexts.values()) {
            if (context.getExpiresAt() <= now) {
                contexts.remove(context.getContextId());
            }
        }
    }
}
