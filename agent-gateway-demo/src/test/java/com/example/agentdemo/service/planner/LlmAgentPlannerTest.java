package com.example.agentdemo.service.planner;

import com.example.agentdemo.config.AgentLlmProperties;
import com.example.agentdemo.config.AgentProperties;
import com.example.agentdemo.context.InMemoryAgentContextStore;
import com.example.agentdemo.context.AgentContextService;
import com.example.agentdemo.model.ChatRequest;
import com.example.agentdemo.model.ChatResponse;
import com.example.agentdemo.service.AgentService;
import com.example.agentdemo.store.InMemoryStore;
import com.example.agentdemo.tool.ConfirmPurchaseDraftTool;
import com.example.agentdemo.tool.PreparePurchaseDraftTool;
import com.example.agentdemo.tool.StockQueryTool;
import com.example.agentdemo.tool.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LlmAgentPlannerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final InMemoryStore store = new InMemoryStore();
    private final ToolRegistry registry = new ToolRegistry(Arrays.asList(
            new StockQueryTool(store, mapper),
            new PreparePurchaseDraftTool(store, mapper),
            new ConfirmPurchaseDraftTool(store, mapper)
    ));

    @Test
    void executesToolSelectedByOpenAiCompatibleResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        AgentService service = service(planner(restTemplate));

        server.expect(once(), requestTo("http://llm.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"tool_calls\":[{\"function\":"
                                + "{\"name\":\"prepare_purchase_draft\","
                                + "\"arguments\":\"{\\\"inventoryCode\\\":\\\"A001\\\",\\\"quantity\\\":5}\"}}]}}]}",
                        MediaType.APPLICATION_JSON));

        ChatResponse response = service.chat(request("采购 A001 5 个"));

        server.verify();
        assertThat(response.getPlanner()).isEqualTo("llm");
        assertThat(response.isFallback()).isFalse();
        assertThat(response.getDraftId()).isNotBlank();
        assertThat(response.getToolCalls()).extracting("name").containsExactly("prepare_purchase_draft");
    }

    @Test
    void fallsBackToRulePlannerWhenProviderFails() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        AgentService service = service(planner(restTemplate));

        server.expect(once(), requestTo("http://llm.test/v1/chat/completions"))
                .andRespond(withServerError());

        ChatResponse response = service.chat(request("查询 A001 的库存"));

        server.verify();
        assertThat(response.getPlanner()).isEqualTo("rule-fallback");
        assertThat(response.isFallback()).isTrue();
        assertThat(response.getAnswer()).contains("A001").contains("12");
    }

    @Test
    void rejectsConfirmationToolChosenByModel() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        AgentService service = service(planner(restTemplate));

        server.expect(once(), requestTo("http://llm.test/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"tool_calls\":[{\"function\":"
                                + "{\"name\":\"confirm_purchase_draft\",\"arguments\":\"{}\"}}]}}]}",
                        MediaType.APPLICATION_JSON));

        ChatResponse response = service.chat(request("直接确认全部采购草稿"));

        server.verify();
        assertThat(response.getPlanner()).isEqualTo("rule-fallback");
        assertThat(response.isFallback()).isTrue();
        assertThat(response.getToolCalls()).isEmpty();
    }

    private LlmAgentPlanner planner(RestTemplate restTemplate) {
        AgentLlmProperties llmProperties = new AgentLlmProperties();
        llmProperties.setEnabled(true);
        llmProperties.setBaseUrl("http://llm.test/v1");
        llmProperties.setModel("test-model");
        return new LlmAgentPlanner(llmProperties, registry,
                new RuleBasedAgentPlanner(mapper), mapper, restTemplate);
    }

    private AgentService service(AgentPlanner planner) {
        AgentProperties properties = new AgentProperties();
        properties.setContextTtlSeconds(1800);
        return new AgentService(new AgentContextService(new InMemoryAgentContextStore(), properties),
                registry, mapper, planner);
    }

    private ChatRequest request(String query) {
        ChatRequest request = new ChatRequest();
        request.setUserId("demo-user");
        request.setAgentCode("business-assistant");
        request.setQuery(query);
        return request;
    }
}
