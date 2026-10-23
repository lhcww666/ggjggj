package com.example.controller;

import com.example.agentdemo.config.AgentProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final AgentProperties agentProperties;

    @Value("${demo.storage:jdbc}")
    private String demoStorage;

    @Value("${rag.vector-store:memory}")
    private String vectorStore;

    public HealthController(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("status", "UP");
        result.put("service", "enterprise-agent-api");
        result.put("agentStorage", agentProperties.getStorage());
        result.put("contextStore", agentProperties.getContextStore());
        result.put("businessStorage", demoStorage);
        result.put("vectorStore", vectorStore);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
