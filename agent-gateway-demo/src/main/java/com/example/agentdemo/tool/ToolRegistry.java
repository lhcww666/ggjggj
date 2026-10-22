package com.example.agentdemo.tool;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public ToolRegistry(List<Tool> toolList) {
        for (Tool tool : toolList) {
            if (tools.put(tool.name(), tool) != null) {
                throw new IllegalStateException("重复的工具名: " + tool.name());
            }
        }
    }

    public Tool find(String name) {
        return tools.get(name);
    }

    public Map<String, Tool> all() {
        return tools;
    }
}
