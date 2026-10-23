package com.example.agentdemo.team;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class SpecialistAgent {

    private final String code;
    private final String displayName;
    private final String description;
    private final Set<String> allowedTools;

    public SpecialistAgent(String code, String displayName, String description, String... allowedTools) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        Set<String> tools = new LinkedHashSet<String>();
        Collections.addAll(tools, allowedTools);
        this.allowedTools = Collections.unmodifiableSet(tools);
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getAllowedTools() {
        return allowedTools;
    }
}
