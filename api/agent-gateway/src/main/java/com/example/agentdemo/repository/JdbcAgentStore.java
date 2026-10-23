package com.example.agentdemo.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "agent", name = "storage", havingValue = "mysql")
public class JdbcAgentStore implements AgentStore {

    private final JdbcTemplate jdbc;

    public JdbcAgentStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Integer stock(String inventoryCode) {
        List<Integer> values = jdbc.query(
                "SELECT quantity FROM agent_stock WHERE inventory_code = ?",
                new Object[]{inventoryCode},
                (rs, rowNum) -> rs.getInt("quantity")
        );
        return values.isEmpty() ? null : values.get(0);
    }

    @Override
    public String createDraft(String inventoryCode, int quantity) {
        String draftId = UUID.randomUUID().toString();
        jdbc.update(
                "INSERT INTO agent_purchase_draft (draft_id, inventory_code, quantity, status) "
                        + "VALUES (?, ?, ?, 'DRAFT')",
                draftId, inventoryCode, quantity
        );
        return draftId;
    }

    @Override
    @Transactional
    public String confirm(String draftId) {
        List<String> existing = jdbc.query(
                "SELECT order_id FROM agent_purchase_order WHERE draft_id = ?",
                new Object[]{draftId},
                (rs, rowNum) -> rs.getString("order_id")
        );
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        List<DraftRow> drafts = jdbc.query(
                "SELECT inventory_code, quantity FROM agent_purchase_draft WHERE draft_id = ?",
                new Object[]{draftId},
                (rs, rowNum) -> new DraftRow(rs.getString("inventory_code"), rs.getInt("quantity"))
        );
        if (drafts.isEmpty()) {
            return null;
        }

        DraftRow draft = drafts.get(0);
        String orderId = "PO-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update(
                "INSERT IGNORE INTO agent_purchase_order "
                        + "(draft_id, order_id, inventory_code, quantity) VALUES (?, ?, ?, ?)",
                draftId, orderId, draft.inventoryCode, draft.quantity
        );
        jdbc.update("UPDATE agent_purchase_draft SET status = 'CONFIRMED' WHERE draft_id = ?", draftId);

        return jdbc.queryForObject(
                "SELECT order_id FROM agent_purchase_order WHERE draft_id = ?",
                new Object[]{draftId},
                String.class
        );
    }

    private static final class DraftRow {
        private final String inventoryCode;
        private final int quantity;

        private DraftRow(String inventoryCode, int quantity) {
            this.inventoryCode = inventoryCode;
            this.quantity = quantity;
        }
    }
}
