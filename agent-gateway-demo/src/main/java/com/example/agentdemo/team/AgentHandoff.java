package com.example.agentdemo.team;

public class AgentHandoff {

    private final String fromAgent;
    private final String toAgent;
    private final String reason;

    public AgentHandoff(String fromAgent, String toAgent, String reason) {
        this.fromAgent = fromAgent;
        this.toAgent = toAgent;
        this.reason = reason;
    }

    public String getFromAgent() {
        return fromAgent;
    }

    public String getToAgent() {
        return toAgent;
    }

    public String getReason() {
        return reason;
    }
}
