package com.example.agentdemo.tool;

import com.example.agentdemo.context.AgentContext;
import com.example.agentdemo.repository.AgentStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class StockQueryTool implements Tool {

    private final AgentStore store;
    private final ObjectMapper mapper;

    public StockQueryTool(AgentStore store, ObjectMapper mapper) {
        this.store = store;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "query_stock";
    }

    @Override
    public String description() {
        return "按精确物料编码查询当前库存。只接受 inventoryCode，不接受物料名称。";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("inventoryCode").put("type", "string");
        schema.putArray("required").add("inventoryCode");
        return schema;
    }

    @Override
    public ToolResult execute(AgentContext context, ObjectNode input) {
        String code = input.path("inventoryCode").asText("");
        Integer quantity = store.stock(code);
        ObjectNode data = mapper.createObjectNode();
        data.put("inventoryCode", code);
        if (quantity == null) {
            data.put("found", false);
            return ToolResult.error("未找到物料 " + code, data);
        }
        data.put("found", true);
        data.put("quantity", quantity);
        return ToolResult.ok("物料 " + code + " 当前库存为 " + quantity, data);
    }
}
