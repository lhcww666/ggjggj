package com.example.integrations.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 可选 LLM 回答生成器：
 * - 未配置 rag.llm.enabled=true 时使用模板；
 * - 配置 OpenAI-Compatible /chat/completions 后使用真实 LLM；
 * - LLM 调用失败时降级到模板，保证演示可用。
 */
@Primary
@Component
public class HybridAnswerGenerator implements AnswerGenerator {

    private final TemplateAnswerGenerator template;
    private final RagLlmProperties properties;
    private final ObjectMapper mapper;

    public HybridAnswerGenerator(TemplateAnswerGenerator template,
                                 RagLlmProperties properties, ObjectMapper mapper) {
        this.template = template;
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override
    public String generate(String query, List<Map<String, Object>> sources) {
        if (!properties.isEnabled() || isBlank(properties.getApiKey()) || isBlank(properties.getBaseUrl())) {
            return template.generate(query, sources);
        }
        try {
            String response = callLlm(query, sources);
            return isBlank(response) ? template.generate(query, sources) : response;
        } catch (Exception exception) {
            return template.generate(query, sources);
        }
    }

    private String callLlm(String query, List<Map<String, Object>> sources) throws Exception {
        String endpoint = properties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        ObjectNode body = mapper.createObjectNode();
        body.put("model", properties.getModel());
        body.put("temperature", 0.2);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system")
                .put("content", "你是企业知识库助手。只能依据提供的资料回答；"
                        + "资料不足时明确说不知道，并给出来源标题。不要编造。");
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < Math.min(sources.size(), 4); i++) {
            Map<String, Object> source = sources.get(i);
            context.append("[").append(source.get("title")).append("] ")
                    .append(source.get("snippet")).append("\n");
        }
        messages.addObject().put("role", "user")
                .put("content", "问题：" + query + "\n参考资料：\n" + context);

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(Math.max(1, properties.getTimeoutSeconds()) * 1000);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + properties.getApiKey());
        try (OutputStream output = connection.getOutputStream()) {
            output.write(mapper.writeValueAsBytes(body));
        }
        if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
            throw new IllegalStateException("LLM HTTP " + connection.getResponseCode());
        }
        JsonNode response = mapper.readTree(read(connection.getInputStream()));
        return response.path("choices").path(0).path("message").path("content").asText("");
    }

    private String read(InputStream input) throws Exception {
        try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = source.read(buffer)) >= 0) {
                output.write(buffer, 0, length);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
