package com.example.agentdemo.controller;

import com.example.agentdemo.config.AgentProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final AgentProperties properties;

    public HealthController(AgentProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("status", "UP");
        result.put("service", "agent-gateway");
        result.put("storage", properties.getStorage());
        result.put("contextStore", properties.getContextStore());
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
