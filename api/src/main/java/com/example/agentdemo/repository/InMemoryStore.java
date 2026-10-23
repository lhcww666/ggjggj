package com.example.agentdemo.repository;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 学习版内存数据。
 * 生产版应替换为 MySQL，并把写操作放进事务。
 */
@Component
@ConditionalOnProperty(prefix = "agent", name = "storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryStore implements AgentStore {

    private final Map<String, Integer> stocks = new ConcurrentHashMap<>();
    private final Map<String, Draft> drafts = new ConcurrentHashMap<>();
    private final Map<String, String> confirmedOrders = new ConcurrentHashMap<>();

    public InMemoryStore() {
        stocks.put("A001", 12);
        stocks.put("A002", 3);
        stocks.put("B100", 0);
    }

    @Override
    public Integer stock(String inventoryCode) {
        return stocks.get(inventoryCode);
    }

    @Override
    public synchronized String createDraft(String inventoryCode, int quantity) {
        String draftId = UUID.randomUUID().toString();
        Draft draft = new Draft(draftId, inventoryCode, quantity);
        drafts.put(draftId, draft);
        return draftId;
    }

    public Draft draft(String draftId) {
        return drafts.get(draftId);
    }

    @Override
    public synchronized String confirm(String draftId) {
        String existing = confirmedOrders.get(draftId);
        if (existing != null) {
            return existing;
        }
        Draft draft = drafts.get(draftId);
        if (draft == null) {
            return null;
        }
        String orderId = "PO-" + UUID.randomUUID().toString().substring(0, 8);
        confirmedOrders.put(draftId, orderId);
        return orderId;
    }

    public static final class Draft {
        private final String draftId;
        private final String inventoryCode;
        private final int quantity;

        public Draft(String draftId, String inventoryCode, int quantity) {
            this.draftId = draftId;
            this.inventoryCode = inventoryCode;
            this.quantity = quantity;
        }

        public String getDraftId() {
            return draftId;
        }

        public String getInventoryCode() {
            return inventoryCode;
        }

        public int getQuantity() {
            return quantity;
        }
    }
}
