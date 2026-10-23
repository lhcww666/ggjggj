package com.example.agentdemo.context;

public interface AgentContextStore {

    void save(AgentContext context);

    AgentContext find(String contextId);

    void delete(String contextId);

    void deleteExpired(long now);
}
