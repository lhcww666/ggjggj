package com.example.agentdemo.service.planner;

import com.example.agentdemo.context.AgentContext;
import com.example.agentdemo.model.ChatRequest;

import java.util.Set;

public interface AgentPlanner {

    AgentPlan plan(ChatRequest request, AgentContext context);

    default AgentPlan plan(ChatRequest request, AgentContext context, Set<String> allowedTools) {
        AgentPlan plan = plan(request, context);
        if (plan.getToolName() != null && allowedTools != null && !allowedTools.contains(plan.getToolName())) {
            return AgentPlan.answer("scope-blocked", "当前 Agent 无权执行该工具，请切换对应专家 Agent。", plan.isFallback());
        }
        return plan;
    }
}
