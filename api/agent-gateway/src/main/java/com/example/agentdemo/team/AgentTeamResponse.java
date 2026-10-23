package com.example.agentdemo.team;

import com.example.agentdemo.model.ChatResponse;

import java.util.List;

public class AgentTeamResponse {

    private final String selectedAgent;
    private final String supervisorReason;
    private final List<AgentHandoff> handoffs;
    private final ChatResponse result;

    public AgentTeamResponse(String selectedAgent, String supervisorReason,
                             List<AgentHandoff> handoffs, ChatResponse result) {
        this.selectedAgent = selectedAgent;
        this.supervisorReason = supervisorReason;
        this.handoffs = handoffs;
        this.result = result;
    }

    public String getSelectedAgent() {
        return selectedAgent;
    }

    public String getSupervisorReason() {
        return supervisorReason;
    }

    public List<AgentHandoff> getHandoffs() {
        return handoffs;
    }

    public ChatResponse getResult() {
        return result;
    }
}
