package com.example.agentdemo.service.planner;

import com.example.agentdemo.context.AgentContext;
import com.example.agentdemo.model.ChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 无外部模型时的确定性规划器。
 *
 * 它只处理演示工具需要的少量意图，作为默认模式和 LLM 故障降级路径。
 */
@Component
public class RuleBasedAgentPlanner implements AgentPlanner {

    private static final Pattern INVENTORY_CODE = Pattern.compile("\\b([A-Za-z]\\d{3,})\\b");
    private static final Pattern QUANTITY_WITH_UNIT = Pattern.compile("(\\d+)\\s*(?:个|件|台|套)");
    private static final Pattern PURCHASE_QUANTITY = Pattern.compile("(?:采购|购买|买|补货)\\s*[A-Za-z]\\d{3,}\\s*(\\d+)");

    private final ObjectMapper mapper;

    public RuleBasedAgentPlanner(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AgentPlan plan(ChatRequest request, AgentContext context) {
        String query = request.getQuery() == null ? "" : request.getQuery();
        String inventoryCode = firstNonBlank(request.getInventoryCode(), extractInventoryCode(query));
        boolean stockIntent = containsAny(query, "库存", "查询", "还有多少", "剩余");
        boolean purchaseIntent = containsAny(query, "采购", "草稿", "购买", "补货");

        if (inventoryCode != null && stockIntent) {
            ObjectNode input = mapper.createObjectNode().put("inventoryCode", inventoryCode);
            return AgentPlan.tool("rule", "query_stock", input);
        }

        if (inventoryCode != null && purchaseIntent) {
            Integer quantity = request.getQuantity() == null ? extractQuantity(query) : request.getQuantity();
            ObjectNode input = mapper.createObjectNode()
                    .put("inventoryCode", inventoryCode)
                    .put("quantity", quantity == null ? 0 : quantity);
            return AgentPlan.tool("rule", "prepare_purchase_draft", input);
        }

        return AgentPlan.answer(
                "rule",
                "请提供物料编码并说明库存查询或采购需求，例如：查询 A001 的库存。",
                false
        );
    }

    private String extractInventoryCode(String query) {
        Matcher matcher = INVENTORY_CODE.matcher(query);
        return matcher.find() ? matcher.group(1).toUpperCase() : null;
    }

    private Integer extractQuantity(String query) {
        Matcher matcher = QUANTITY_WITH_UNIT.matcher(query);
        if (!matcher.find()) {
            matcher = PURCHASE_QUANTITY.matcher(query);
            if (!matcher.find()) {
                return null;
            }
        }
        return Integer.valueOf(matcher.group(1));
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        return second;
    }
}
