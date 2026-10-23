package com.example.agentdemo.controller;

import com.example.agentdemo.context.AgentContext;
import com.example.agentdemo.security.ToolAuthService;
import com.example.agentdemo.tool.Tool;
import com.example.agentdemo.tool.ToolRegistry;
import com.example.agentdemo.tool.ToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final ToolRegistry registry;
    private final ToolAuthService authService;
    private final ObjectMapper mapper;

    public ToolController(ToolRegistry registry, ToolAuthService authService, ObjectMapper mapper) {
        this.registry = registry;
        this.authService = authService;
        this.mapper = mapper;
    }

    @PostMapping("/{name}")
    public ResponseEntity<JsonNode> call(
            @PathVariable String name,
            @RequestHeader(value = "X-Agent-Tool-Key", required = false) String toolKey,
            @RequestHeader(value = "X-Agent-Context-Id", required = false) String contextId,
            @RequestBody(required = false) JsonNode body) {

        if (!authService.authenticate(toolKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(error("UNAUTHORIZED", "工具认证失败"));
        }
        Tool tool = registry.find(name);
        if (tool == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(error("UNKNOWN_TOOL", "未知工具: " + name));
        }
        try {
            AgentContext context = authService.requireContext(contextId);
            ObjectNode input = body == null || !body.isObject()
                    ? mapper.createObjectNode()
                    : (ObjectNode) body;
            ToolResult result = tool.execute(context, input);
            ObjectNode response = mapper.createObjectNode();
            response.put("success", result.isSuccess());
            response.put("text", result.getText());
            response.set("data", result.getData());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(error("AGENT_CONTEXT_INVALID", exception.getMessage()));
        }
    }

    private ObjectNode error(String code, String message) {
        return mapper.createObjectNode().put("success", false).put("code", code).put("message", message);
    }
}
