package com.example.agentdemo.security;

import com.example.agentdemo.config.AgentProperties;
import com.example.agentdemo.context.AgentContext;
import com.example.agentdemo.context.AgentContextService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class ToolAuthService {

    private final AgentProperties properties;
    private final AgentContextService contextService;

    public ToolAuthService(AgentProperties properties, AgentContextService contextService) {
        this.properties = properties;
        this.contextService = contextService;
    }

    public boolean authenticate(String suppliedKey) {
        String expected = properties.getToolKey();
        if (isBlank(expected) || suppliedKey == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                suppliedKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    public AgentContext requireContext(String contextId) {
        AgentContext context = contextService.resolve(contextId);
        if (context == null) {
            throw new IllegalArgumentException("用户上下文不存在或已失效");
        }
        return context;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
