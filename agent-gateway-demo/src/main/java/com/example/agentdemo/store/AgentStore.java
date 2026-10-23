package com.example.agentdemo.store;

public interface AgentStore {

    Integer stock(String inventoryCode);

    String createDraft(String inventoryCode, int quantity);

    String confirm(String draftId);
}
