package com.example.agentdemo.tool;

import com.example.agentdemo.context.AgentContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface Tool {

    String name();

    String description();

    ObjectNode inputSchema();

    ToolResult execute(AgentContext context, ObjectNode input);
}
