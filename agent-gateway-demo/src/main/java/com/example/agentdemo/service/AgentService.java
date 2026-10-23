package com.example.agentdemo.service;

import com.example.agentdemo.context.AgentContext;
import com.example.agentdemo.context.AgentContextService;
import com.example.agentdemo.model.ChatRequest;
import com.example.agentdemo.model.ChatResponse;
import com.example.agentdemo.model.ToolCall;
import com.example.agentdemo.service.planner.AgentPlan;
import com.example.agentdemo.service.planner.AgentPlanner;
import com.example.agentdemo.tool.Tool;
import com.example.agentdemo.tool.ToolRegistry;
import com.example.agentdemo.tool.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class AgentService {

    private final AgentContextService contextService;
    private final ToolRegistry registry;
    private final ObjectMapper mapper;
    private final AgentPlanner planner;

    public AgentService(AgentContextService contextService, ToolRegistry registry, ObjectMapper mapper,
                        AgentPlanner planner) {
        this.contextService = contextService;
        this.registry = registry;
        this.mapper = mapper;
        this.planner = planner;
    }

    public ChatResponse chat(ChatRequest request) {
        return chat(request, null);
    }

    public ChatResponse chat(ChatRequest request, Set<String> allowedTools) {
        if (isBlank(request.getQuery())) {
            throw new IllegalArgumentException("query 不能为空");
        }

        AgentContext context = contextService.create(request.getUserId(), request.getAgentCode());
        ChatResponse response = new ChatResponse();
        response.setContextId(context.getContextId());
        response.setConversationId(request.getConversationId() == null
                ? UUID.randomUUID().toString()
                : request.getConversationId());

        if (!isBlank(request.getConfirmDraftId())) {
            if (allowedTools != null && !allowedTools.contains("confirm_purchase_draft")) {
                response.setPlanner("scope-blocked");
                response.setAnswer("当前 Agent 无权确认采购草稿，请切换采购专家 Agent。");
                return response;
            }
            ObjectNode input = mapper.createObjectNode().put("draftId", request.getConfirmDraftId());
            response.setPlanner("explicit-confirmation");
            applyTool("confirm_purchase_draft", context, input, response);
            return response;
        }

        AgentPlan plan = planner.plan(request, context, allowedTools);
        response.setPlanner(plan.getPlanner());
        response.setFallback(plan.isFallback());
        if (!isBlank(plan.getToolName())) {
            applyTool(plan.getToolName(), context, plan.getInput(), response);
        } else {
            response.setAnswer(plan.getAnswer());
        }
        return response;
    }

    private void applyTool(String name, AgentContext context, ObjectNode input, ChatResponse response) {
        Tool tool = registry.find(name);
        if (tool == null) {
            throw new IllegalStateException("未注册工具: " + name);
        }
        ToolResult result = tool.execute(context, input);
        response.setAnswer(result.getText());
        response.getToolCalls().add(new ToolCall(name, input, result.getData(), result.isSuccess()));
        if (result.getData() != null && result.getData().has("draftId")) {
            response.setDraftId(result.getData().path("draftId").asText());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
