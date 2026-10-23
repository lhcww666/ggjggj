package com.example.agentdemo.service.planner;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Planner 的单轮决策结果。
 *
 * toolName 非空时由 AgentService 执行对应 Java 工具；否则直接返回 answer。
 */
public class AgentPlan {

    private final String planner;
    private final String toolName;
    private final ObjectNode input;
    private final String answer;
    private final boolean fallback;

    public AgentPlan(String planner, String toolName, ObjectNode input, String answer, boolean fallback) {
        this.planner = planner;
        this.toolName = toolName;
        this.input = input;
        this.answer = answer;
        this.fallback = fallback;
    }

    public static AgentPlan tool(String planner, String toolName, ObjectNode input) {
        return new AgentPlan(planner, toolName, input, null, false);
    }

    public static AgentPlan answer(String planner, String answer, boolean fallback) {
        return new AgentPlan(planner, null, null, answer, fallback);
    }

    public String getPlanner() {
        return planner;
    }

    public String getToolName() {
        return toolName;
    }

    public ObjectNode getInput() {
        return input;
    }

    public String getAnswer() {
        return answer;
    }

    public boolean isFallback() {
        return fallback;
    }
}
