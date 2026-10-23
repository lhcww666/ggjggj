package com.example.agentdemo.service.planner;

import com.example.agentdemo.context.AgentContext;
import com.example.agentdemo.model.ChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedAgentPlannerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final RuleBasedAgentPlanner planner = new RuleBasedAgentPlanner(mapper);
    private final AgentContext context = new AgentContext("ctx-1", "u-1", "stock-assistant",
            System.currentTimeMillis() + 60_000);

    @Test
    void extractsInventoryAndQuantityFromNaturalLanguage() {
        ChatRequest request = new ChatRequest();
        request.setQuery("帮我采购 a001 5 个");

        AgentPlan plan = planner.plan(request, context);

        assertThat(plan.getToolName()).isEqualTo("prepare_purchase_draft");
        assertThat(plan.getInput().path("inventoryCode").asText()).isEqualTo("A001");
        assertThat(plan.getInput().path("quantity").asInt()).isEqualTo(5);
        assertThat(plan.isFallback()).isFalse();
    }

    @Test
    void extractsCodeForStockQueryWithoutStructuredFields() {
        ChatRequest request = new ChatRequest();
        request.setQuery("A002 还有多少库存");

        AgentPlan plan = planner.plan(request, context);

        assertThat(plan.getToolName()).isEqualTo("query_stock");
        assertThat(plan.getInput().path("inventoryCode").asText()).isEqualTo("A002");
    }

    @Test
    void extractsWaybillNumberForLogisticsQuery() {
        ChatRequest request = new ChatRequest();
        request.setQuery("查询运单 sf202610200001 的物流轨迹");

        AgentPlan plan = planner.plan(request, context);

        assertThat(plan.getToolName()).isEqualTo("query_logistics_route");
        assertThat(plan.getInput().path("waybillNo").asText()).isEqualTo("SF202610200001");
    }

    @Test
    void asksForWaybillWhenLogisticsIntentHasNoNumber() {
        ChatRequest request = new ChatRequest();
        request.setQuery("帮我查询物流轨迹");

        AgentPlan plan = planner.plan(request, context);

        assertThat(plan.getToolName()).isNull();
        assertThat(plan.getAnswer()).contains("运单号");
    }
}
