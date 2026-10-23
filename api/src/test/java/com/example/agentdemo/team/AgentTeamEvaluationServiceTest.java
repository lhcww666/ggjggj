package com.example.agentdemo.team;

import com.example.agentdemo.config.AgentProperties;
import com.example.agentdemo.context.InMemoryAgentContextStore;
import com.example.agentdemo.context.AgentContextService;
import com.example.agentdemo.service.AgentService;
import com.example.agentdemo.service.planner.RuleBasedAgentPlanner;
import com.example.agentdemo.repository.InMemoryStore;
import com.example.agentdemo.tool.ConfirmPurchaseDraftTool;
import com.example.agentdemo.tool.PreparePurchaseDraftTool;
import com.example.agentdemo.tool.StockQueryTool;
import com.example.agentdemo.tool.ToolRegistry;
import com.example.agentdemo.tool.LogisticsRouteTool;
import com.example.integrations.config.IntegrationProperties;
import com.example.integrations.operation.OperationLogService;
import com.example.integrations.repository.MemoryDemoRepository;
import com.example.integrations.sf.SfExpressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentTeamEvaluationServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void routingSuitePassesAllCases() {
        ObjectMapper mapper = new ObjectMapper();
        InMemoryStore store = new InMemoryStore();
        MemoryDemoRepository demoRepository = new MemoryDemoRepository();
        IntegrationProperties integrationProperties = new IntegrationProperties();
        integrationProperties.setSfCheckWord("test-sf-check-word");
        integrationProperties.setSfCustomerCode("TEST_CUSTOMER");
        ToolRegistry tools = new ToolRegistry(Arrays.asList(
                new StockQueryTool(store, mapper),
                new PreparePurchaseDraftTool(store, mapper),
                new ConfirmPurchaseDraftTool(store, mapper),
                new LogisticsRouteTool(new SfExpressService(
                        integrationProperties,
                        mapper,
                        demoRepository,
                        new OperationLogService(demoRepository)
                ), mapper)
        ));
        AgentService agentService = new AgentService(
                new AgentContextService(new InMemoryAgentContextStore(), new AgentProperties()),
                tools,
                mapper,
                new RuleBasedAgentPlanner(mapper)
        );
        AgentTeamService teamService = new AgentTeamService(
                new SpecialistAgentRegistry(),
                agentService
        );

        Map<String, Object> result = new AgentTeamEvaluationService(teamService).evaluate();

        assertThat(result.get("total")).isEqualTo(11);
        assertThat(result.get("hits")).isEqualTo(11);
        assertThat((Double) result.get("passRate")).isEqualTo(1.0);
        List<Map<String, Object>> cases = (List<Map<String, Object>>) result.get("cases");
        assertThat(cases).allMatch(item -> Boolean.TRUE.equals(item.get("passed")));
    }
}
