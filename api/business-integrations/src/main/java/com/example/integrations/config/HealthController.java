package com.example.integrations.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Value("${demo.storage:memory}")
    private String storage;

    @Value("${rag.vector-store:memory}")
    private String vectorStore;

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("status", "UP");
        result.put("service", "business-integrations");
        result.put("storage", storage);
        result.put("vectorStore", vectorStore);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
