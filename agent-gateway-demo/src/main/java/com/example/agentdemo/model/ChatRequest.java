package com.example.agentdemo.model;

public class ChatRequest {

    private String query;
    private String userId;
    private String agentCode;
    private String conversationId;
    private String inventoryCode;
    private Integer quantity;
    private String confirmDraftId;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAgentCode() {
        return agentCode;
    }

    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getInventoryCode() {
        return inventoryCode;
    }

    public void setInventoryCode(String inventoryCode) {
        this.inventoryCode = inventoryCode;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getConfirmDraftId() {
        return confirmDraftId;
    }

    public void setConfirmDraftId(String confirmDraftId) {
        this.confirmDraftId = confirmDraftId;
    }
}
