package com.example.agentdemo.service;

import com.example.agentdemo.context.AgentContext;
import com.example.agentdemo.context.AgentContextService;
import com.example.agentdemo.model.ChatRequest;
import com.example.agentdemo.model.ChatResponse;
import com.example.agentdemo.model.ToolCall;
import com.example.agentdemo.tool.Tool;
import com.example.agentdemo.tool.ToolRegistry;
import com.example.agentdemo.tool.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 学习版“规划器”。
 *
 * 这里用关键词模拟模型选择工具，目的不是冒充大模型，
 * 而是先把 Java 工具网关、上下文、确认流程跑通。
 * 真正接入 Dify 后，应由 Dify 负责意图理解和工具选择。
 */
@Service
public class AgentService {

    private final AgentContextService contextService;
    private final ToolRegistry registry;
    private final ObjectMapper mapper;

    public AgentService(AgentContextService contextService, ToolRegistry registry, ObjectMapper mapper) {
        this.contextService = contextService;
        this.registry = registry;
        this.mapper = mapper;
    }

    public ChatResponse chat(ChatRequest request) {
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
            ObjectNode input = mapper.createObjectNode().put("draftId", request.getConfirmDraftId());
            applyTool("confirm_purchase_draft", context, input, response);
            return response;
        }

        if (request.getInventoryCode() != null
                && (request.getQuery().contains("库存") || request.getQuery().contains("查询"))) {
            ObjectNode input = mapper.createObjectNode().put("inventoryCode", request.getInventoryCode());
            applyTool("query_stock", context, input, response);
            return response;
        }

        if (request.getInventoryCode() != null
                && (request.getQuery().contains("采购") || request.getQuery().contains("草稿"))) {
            ObjectNode input = mapper.createObjectNode()
                    .put("inventoryCode", request.getInventoryCode())
                    .put("quantity", request.getQuantity() == null ? 0 : request.getQuantity());
            applyTool("prepare_purchase_draft", context, input, response);
            return response;
        }

        response.setAnswer("学习版 Demo 只处理库存查询和采购草稿。你可以说：查询 A001 的库存。");
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
