package com.example.agentdemo.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ToolResult {

    private final boolean success;
    private final String text;
    private final ObjectNode data;

    private ToolResult(boolean success, String text, ObjectNode data) {
        this.success = success;
        this.text = text;
        this.data = data;
    }

    public static ToolResult ok(String text, ObjectNode data) {
        return new ToolResult(true, text, data);
    }

    public static ToolResult error(String text, ObjectNode data) {
        return new ToolResult(false, text, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getText() {
        return text;
    }

    public ObjectNode getData() {
        return data;
    }
}
