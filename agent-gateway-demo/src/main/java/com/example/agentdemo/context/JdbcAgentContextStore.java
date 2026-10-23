package com.example.agentdemo.context;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(prefix = "agent", name = "context-store", havingValue = "mysql")
public class JdbcAgentContextStore implements AgentContextStore {

    private final JdbcTemplate jdbc;

    public JdbcAgentContextStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(AgentContext context) {
        jdbc.update(
                "INSERT INTO agent_context (context_id, user_id, agent_code, expires_at) VALUES (?, ?, ?, ?)",
                context.getContextId(), context.getUserId(), context.getAgentCode(), context.getExpiresAt()
        );
    }

    @Override
    public AgentContext find(String contextId) {
        List<AgentContext> rows = jdbc.query(
                "SELECT context_id, user_id, agent_code, expires_at FROM agent_context WHERE context_id = ?",
                new Object[]{contextId},
                (rs, rowNum) -> new AgentContext(
                        rs.getString("context_id"),
                        rs.getString("user_id"),
                        rs.getString("agent_code"),
                        rs.getLong("expires_at")
                )
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public void delete(String contextId) {
        jdbc.update("DELETE FROM agent_context WHERE context_id = ?", contextId);
    }

    @Override
    public void deleteExpired(long now) {
        jdbc.update("DELETE FROM agent_context WHERE expires_at <= ?", now);
    }
}
