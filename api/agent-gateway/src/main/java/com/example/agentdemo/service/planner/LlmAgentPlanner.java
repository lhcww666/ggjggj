package com.example.agentdemo.service.planner;

import com.example.agentdemo.config.AgentLlmProperties;
import com.example.agentdemo.context.AgentContext;
import com.example.agentdemo.model.ChatRequest;
import com.example.agentdemo.tool.Tool;
import com.example.agentdemo.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

/**
 * 可选的 OpenAI-Compatible Tool Calling 规划器。
 *
 * 模型只负责选择工具和生成参数，工具执行、用户上下文、权限与确认流程仍留在 Java。
 */
@Primary
@Component
@ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "true")
public class LlmAgentPlanner implements AgentPlanner {

    private static final Logger log = LoggerFactory.getLogger(LlmAgentPlanner.class);
    private static final String CONFIRM_TOOL = "confirm_purchase_draft";

    private final AgentLlmProperties properties;
    private final ToolRegistry registry;
    private final RuleBasedAgentPlanner fallbackPlanner;
    private final ObjectMapper mapper;
    private final RestTemplate restTemplate;

    public LlmAgentPlanner(AgentLlmProperties properties, ToolRegistry registry,
                           RuleBasedAgentPlanner fallbackPlanner, ObjectMapper mapper) {
        this(properties, registry, fallbackPlanner, mapper, createRestTemplate(properties));
    }

    LlmAgentPlanner(AgentLlmProperties properties, ToolRegistry registry,
                    RuleBasedAgentPlanner fallbackPlanner, ObjectMapper mapper,
                    RestTemplate restTemplate) {
        this.properties = properties;
        this.registry = registry;
        this.fallbackPlanner = fallbackPlanner;
        this.mapper = mapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public AgentPlan plan(ChatRequest request, AgentContext context) {
        return plan(request, context, null);
    }

    @Override
    public AgentPlan plan(ChatRequest request, AgentContext context, Set<String> allowedTools) {
        if (isBlank(properties.getBaseUrl()) || isBlank(properties.getModel())) {
            return fallback(request, context, allowedTools, "LLM base-url 或 model 未配置");
        }

        try {
            ObjectNode body = buildRequest(request, allowedTools);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (!isBlank(properties.getApiKey())) {
                headers.setBearerAuth(properties.getApiKey().trim());
            }

            String endpoint = properties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint, HttpMethod.POST, new HttpEntity<String>(mapper.writeValueAsString(body), headers),
                    String.class
            );
            return parseResponse(response.getBody(), request, context, allowedTools);
        } catch (Exception exception) {
            log.warn("LLM planner 调用失败，切换到规则规划器: {}", exception.getMessage());
            return fallback(request, context, allowedTools, exception.getMessage());
        }
    }

    private ObjectNode buildRequest(ChatRequest request, Set<String> allowedTools) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", properties.getModel());
        body.put("temperature", properties.getTemperature());

        ArrayNode messages = body.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", "你是企业业务 Agent 的规划器。只能从提供的工具中选择一个，"
                        + "参数必须使用用户明确提供的物料编码和数量。不得编造业务数据，"
                        + "不得调用确认类工具；正式写操作必须由用户通过确认字段触发。");

        StringBuilder user = new StringBuilder(request.getQuery());
        if (!isBlank(request.getInventoryCode())) {
            user.append("\n结构化提示：inventoryCode=").append(request.getInventoryCode().trim());
        }
        if (request.getQuantity() != null) {
            user.append("\n结构化提示：quantity=").append(request.getQuantity());
        }
        messages.addObject().put("role", "user").put("content", user.toString());

        ArrayNode tools = body.putArray("tools");
        for (Tool tool : registry.all().values()) {
            if (CONFIRM_TOOL.equals(tool.name())
                    || (allowedTools != null && !allowedTools.contains(tool.name()))) {
                continue;
            }
            ObjectNode item = tools.addObject();
            item.put("type", "function");
            ObjectNode function = item.putObject("function");
            function.put("name", tool.name());
            function.put("description", tool.description());
            function.set("parameters", tool.inputSchema());
        }
        body.put("tool_choice", "auto");
        return body;
    }

    private AgentPlan parseResponse(String responseBody, ChatRequest request, AgentContext context,
                                    Set<String> allowedTools) throws Exception {
        if (isBlank(responseBody)) {
            return fallback(request, context, allowedTools, "LLM 返回空响应");
        }

        JsonNode message = mapper.readTree(responseBody)
                .path("choices").path(0).path("message");
        JsonNode toolCalls = message.path("tool_calls");
        if (!toolCalls.isArray() || toolCalls.size() == 0) {
            String content = message.path("content").asText("");
            if (isBlank(content)) {
                return fallback(request, context, allowedTools, "LLM 未返回工具调用或回答");
            }
            return AgentPlan.answer("llm", content.trim(), false);
        }

        JsonNode function = toolCalls.path(0).path("function");
        String toolName = function.path("name").asText("");
        if (CONFIRM_TOOL.equals(toolName) || registry.find(toolName) == null
                || (allowedTools != null && !allowedTools.contains(toolName))) {
            return fallback(request, context, allowedTools, "LLM 选择了未允许的工具: " + toolName);
        }

        JsonNode argumentsNode = mapper.readTree(function.path("arguments").asText("{}"));
        if (!argumentsNode.isObject()) {
            return fallback(request, context, allowedTools, "LLM 工具参数不是 JSON 对象");
        }
        return AgentPlan.tool("llm", toolName, (ObjectNode) argumentsNode);
    }

    private AgentPlan fallback(ChatRequest request, AgentContext context, Set<String> allowedTools, String reason) {
        AgentPlan rulePlan = fallbackPlanner.plan(request, context, allowedTools);
        log.warn("LLM 降级原因: {}", reason);
        return new AgentPlan(
                "rule-fallback",
                rulePlan.getToolName(),
                rulePlan.getInput(),
                rulePlan.getAnswer(),
                true
        );
    }

    private static RestTemplate createRestTemplate(AgentLlmProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeoutMillis());
        factory.setReadTimeout(properties.getTimeoutMillis());
        return new RestTemplate(factory);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
