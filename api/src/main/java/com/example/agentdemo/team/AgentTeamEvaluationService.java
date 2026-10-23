package com.example.agentdemo.team;

import com.example.agentdemo.model.ChatRequest;
import com.example.agentdemo.model.ToolCall;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentTeamEvaluationService {

    private final AgentTeamService teamService;

    public AgentTeamEvaluationService(AgentTeamService teamService) {
        this.teamService = teamService;
    }

    public Map<String, Object> evaluate() {
        List<EvaluationCase> cases = buildCases();
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        int hits = 0;

        for (EvaluationCase test : cases) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("name", test.name);
            item.put("query", test.query);
            item.put("expectedAgent", test.expectedAgent);
            item.put("expectedPlanner", test.expectedPlanner);
            item.put("expectedTool", test.expectedTool);

            try {
                ChatRequest request = new ChatRequest();
                request.setUserId("agent-team-evaluator");
                request.setQuery(test.query);
                request.setAgentCode(test.agentCode);
                AgentTeamResponse response = teamService.chat(request);
                String actualTool = firstToolName(response.getResult().getToolCalls());
                boolean passed = equals(test.expectedAgent, response.getSelectedAgent())
                        && equals(test.expectedPlanner, response.getResult().getPlanner())
                        && equals(test.expectedTool, actualTool);

                item.put("actualAgent", response.getSelectedAgent());
                item.put("actualPlanner", response.getResult().getPlanner());
                item.put("actualTool", actualTool);
                item.put("answer", response.getResult().getAnswer());
                item.put("passed", passed);
                if (passed) {
                    hits++;
                }
            } catch (Exception exception) {
                item.put("actualAgent", null);
                item.put("actualPlanner", null);
                item.put("actualTool", null);
                item.put("error", exception.getMessage());
                item.put("passed", false);
            }
            results.add(item);
        }

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("suite", "agent-team-routing");
        summary.put("total", cases.size());
        summary.put("hits", hits);
        summary.put("passRate", cases.isEmpty() ? 0.0 : (double) hits / cases.size());
        summary.put("cases", results);
        return summary;
    }

    private List<EvaluationCase> buildCases() {
        List<EvaluationCase> cases = new ArrayList<EvaluationCase>();
        cases.add(new EvaluationCase(
                "库存路由-A001",
                "A001 还有多少库存",
                null,
                "inventory-specialist",
                "rule",
                "query_stock"
        ));
        cases.add(new EvaluationCase(
                "库存路由-B100",
                "查询 B100 的库存",
                null,
                "inventory-specialist",
                "rule",
                "query_stock"
        ));
        cases.add(new EvaluationCase(
                "采购路由",
                "采购 A001 5 个",
                null,
                "purchase-specialist",
                "rule",
                "prepare_purchase_draft"
        ));
        cases.add(new EvaluationCase(
                "购买意图识别",
                "帮我购买 A002 2 件",
                null,
                "purchase-specialist",
                "rule",
                "prepare_purchase_draft"
        ));
        cases.add(new EvaluationCase(
                "主管兜底",
                "你好，帮我看看",
                null,
                "supervisor",
                "rule",
                null
        ));
        cases.add(new EvaluationCase(
                "采购专家越权查库存",
                "A001 还有多少库存",
                "purchase-specialist",
                "purchase-specialist",
                "scope-blocked",
                null
        ));
        cases.add(new EvaluationCase(
                "库存专家越权建采购草稿",
                "采购 A001 2 个",
                "inventory-specialist",
                "inventory-specialist",
                "scope-blocked",
                null
        ));
        cases.add(new EvaluationCase(
                "采购缺少参数",
                "我要采购",
                null,
                "purchase-specialist",
                "rule",
                null
        ));
        return cases;
    }

    private String firstToolName(List<ToolCall> toolCalls) {
        return toolCalls == null || toolCalls.isEmpty() ? null : toolCalls.get(0).getName();
    }

    private boolean equals(String expected, String actual) {
        return expected == null ? actual == null : expected.equals(actual);
    }

    private static final class EvaluationCase {
        private final String name;
        private final String query;
        private final String agentCode;
        private final String expectedAgent;
        private final String expectedPlanner;
        private final String expectedTool;

        private EvaluationCase(String name, String query, String agentCode, String expectedAgent,
                               String expectedPlanner, String expectedTool) {
            this.name = name;
            this.query = query;
            this.agentCode = agentCode;
            this.expectedAgent = expectedAgent;
            this.expectedPlanner = expectedPlanner;
            this.expectedTool = expectedTool;
        }
    }
}
