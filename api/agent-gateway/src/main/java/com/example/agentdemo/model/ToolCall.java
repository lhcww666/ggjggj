package com.example.agentdemo.model;

import com.fasterxml.jackson.databind.JsonNode;

public class ToolCall {

    private String name;
    private JsonNode input;
    private JsonNode output;
    private boolean success;

    public ToolCall(String name, JsonNode input, JsonNode output, boolean success) {
        this.name = name;
        this.input = input;
        this.output = output;
        this.success = success;
    }

    public String getName() {
        return name;
    }

    public JsonNode getInput() {
        return input;
    }

    public JsonNode getOutput() {
        return output;
    }

    public boolean isSuccess() {
        return success;
    }
}
