package com.example.agentdemo.team;

import com.example.agentdemo.config.AgentProperties;
import com.example.agentdemo.context.AgentContextService;
import com.example.agentdemo.model.ChatRequest;
import com.example.agentdemo.service.AgentService;
import com.example.agentdemo.service.planner.RuleBasedAgentPlanner;
import com.example.agentdemo.store.InMemoryStore;
import com.example.agentdemo.tool.ConfirmPurchaseDraftTool;
import com.example.agentdemo.tool.PreparePurchaseDraftTool;
import com.example.agentdemo.tool.StockQueryTool;
import com.example.agentdemo.tool.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AgentTeamServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final InMemoryStore store = new InMemoryStore();
    private final ToolRegistry tools = new ToolRegistry(Arrays.asList(
            new StockQueryTool(store, mapper),
            new PreparePurchaseDraftTool(store, mapper),
            new ConfirmPurchaseDraftTool(store, mapper)
    ));
    private final AgentProperties properties = new AgentProperties();
    private final AgentService agentService = new AgentService(
            new AgentContextService(properties),
            tools,
            mapper,
            new RuleBasedAgentPlanner(mapper)
    );
    private final AgentTeamService teamService = new AgentTeamService(
            new SpecialistAgentRegistry(),
            agentService
    );

    @Test
    void routesInventoryQuestionToInventorySpecialist() {
        AgentTeamResponse response = teamService.chat(request("A002 还有多少库存", null));

        assertThat(response.getSelectedAgent()).isEqualTo("inventory-specialist");
        assertThat(response.getHandoffs()).hasSize(1);
        assertThat(response.getResult().getAnswer()).contains("A002").contains("3");
        assertThat(response.getResult().getToolCalls()).extracting("name").containsExactly("query_stock");
    }

    @Test
    void routesPurchaseDraftToPurchaseSpecialist() {
        AgentTeamResponse response = teamService.chat(request("采购 A001 5 个", null));

        assertThat(response.getSelectedAgent()).isEqualTo("purchase-specialist");
        assertThat(response.getResult().getDraftId()).isNotBlank();
        assertThat(response.getResult().getToolCalls()).extracting("name")
                .containsExactly("prepare_purchase_draft");
    }

    @Test
    void explicitConfirmationStaysWithPurchaseSpecialist() {
        AgentTeamResponse draft = teamService.chat(request("采购 A001 2 个", null));

        ChatRequest confirm = request("确认创建", null);
        confirm.setConfirmDraftId(draft.getResult().getDraftId());
        AgentTeamResponse confirmed = teamService.chat(confirm);

        assertThat(confirmed.getSelectedAgent()).isEqualTo("purchase-specialist");
        assertThat(confirmed.getResult().getPlanner()).isEqualTo("explicit-confirmation");
        assertThat(confirmed.getResult().getToolCalls()).extracting("name")
                .containsExactly("confirm_purchase_draft");
    }

    @Test
    void specialistCannotUseAnotherAgentsTool() {
        AgentTeamResponse response = teamService.chat(
                request("A001 还有多少库存", "purchase-specialist")
        );

        assertThat(response.getSelectedAgent()).isEqualTo("purchase-specialist");
        assertThat(response.getResult().getPlanner()).isEqualTo("scope-blocked");
        assertThat(response.getResult().getToolCalls()).isEmpty();
    }

    @Test
    void unknownIntentStaysWithSupervisor() {
        AgentTeamResponse response = teamService.chat(request("你好，帮我看看", null));

        assertThat(response.getSelectedAgent()).isEqualTo("supervisor");
        assertThat(response.getHandoffs()).isEmpty();
        assertThat(response.getResult().getAnswer()).contains("物料编码");
    }

    private ChatRequest request(String query, String agentCode) {
        ChatRequest request = new ChatRequest();
        request.setUserId("team-user");
        request.setAgentCode(agentCode);
        request.setQuery(query);
        return request;
    }
}
