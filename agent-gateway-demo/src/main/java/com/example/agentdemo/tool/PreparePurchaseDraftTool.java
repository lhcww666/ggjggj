package com.example.agentdemo.tool;

import com.example.agentdemo.context.AgentContext;
import com.example.agentdemo.store.InMemoryStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class PreparePurchaseDraftTool implements Tool {

    private final InMemoryStore store;
    private final ObjectMapper mapper;

    public PreparePurchaseDraftTool(InMemoryStore store, ObjectMapper mapper) {
        this.store = store;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "prepare_purchase_draft";
    }

    @Override
    public String description() {
        return "校验物料和数量，生成采购草稿并返回 draftId。此工具不会创建正式采购订单。";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("inventoryCode").put("type", "string");
        properties.putObject("quantity").put("type", "integer").put("minimum", 1);
        schema.putArray("required").add("inventoryCode").add("quantity");
        return schema;
    }

    @Override
    public ToolResult execute(AgentContext context, ObjectNode input) {
        String code = input.path("inventoryCode").asText("");
        int quantity = input.path("quantity").asInt(0);
        ObjectNode data = mapper.createObjectNode();
        if (store.stock(code) == null) {
            data.put("code", "INVENTORY_NOT_FOUND");
            return ToolResult.error("物料不存在，不能创建采购草稿", data);
        }
        if (quantity <= 0) {
            data.put("code", "INVALID_QUANTITY");
            return ToolResult.error("采购数量必须大于 0", data);
        }
        InMemoryStore.Draft draft = store.createDraft(code, quantity);
        data.put("code", "ACTION_CONFIRMATION_REQUIRED");
        data.put("draftId", draft.getDraftId());
        data.put("inventoryCode", code);
        data.put("quantity", quantity);
        data.put("message", "草稿已生成，必须由用户确认后才能创建正式订单");
        return ToolResult.ok("采购草稿已生成，请确认 draftId=" + draft.getDraftId(), data);
    }
}
