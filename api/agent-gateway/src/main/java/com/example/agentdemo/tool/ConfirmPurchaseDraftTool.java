package com.example.agentdemo.tool;

import com.example.agentdemo.context.AgentContext;
import com.example.agentdemo.repository.AgentStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class ConfirmPurchaseDraftTool implements Tool {

    private final AgentStore store;
    private final ObjectMapper mapper;

    public ConfirmPurchaseDraftTool(AgentStore store, ObjectMapper mapper) {
        this.store = store;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "confirm_purchase_draft";
    }

    @Override
    public String description() {
        return "确认并执行采购草稿。生产环境应校验用户归属、草稿有效期和幂等键。";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("draftId").put("type", "string");
        schema.putArray("required").add("draftId");
        return schema;
    }

    @Override
    public ToolResult execute(AgentContext context, ObjectNode input) {
        String draftId = input.path("draftId").asText("");
        ObjectNode data = mapper.createObjectNode();
        if (draftId == null || draftId.trim().isEmpty()) {
            data.put("code", "INVALID_DRAFT_ID");
            return ToolResult.error("draftId 不能为空", data);
        }
        String orderId = store.confirm(draftId);
        if (orderId == null) {
            data.put("code", "DRAFT_NOT_FOUND");
            return ToolResult.error("草稿不存在或已失效", data);
        }
        data.put("code", "ACTION_EXECUTED");
        data.put("draftId", draftId);
        data.put("orderId", orderId);
        return ToolResult.ok("采购订单已创建，orderId=" + orderId, data);
    }
}
