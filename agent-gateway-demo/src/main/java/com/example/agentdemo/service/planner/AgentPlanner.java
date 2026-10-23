package com.example.agentdemo.service.planner;

import com.example.agentdemo.context.AgentContext;
import com.example.agentdemo.model.ChatRequest;

public interface AgentPlanner {

    AgentPlan plan(ChatRequest request, AgentContext context);
}
