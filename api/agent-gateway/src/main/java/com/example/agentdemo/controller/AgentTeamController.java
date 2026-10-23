package com.example.agentdemo.controller;

import com.example.agentdemo.config.AgentProperties;
import com.example.agentdemo.model.ChatRequest;
import com.example.agentdemo.team.AgentTeamResponse;
import com.example.agentdemo.team.AgentTeamEvaluationService;
import com.example.agentdemo.team.AgentTeamService;
import com.example.agentdemo.team.SpecialistAgent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/agent/team")
public class AgentTeamController {

    private final AgentTeamService teamService;
    private final AgentTeamEvaluationService evaluationService;
    private final AgentProperties properties;

    public AgentTeamController(AgentTeamService teamService,
                               AgentTeamEvaluationService evaluationService,
                               AgentProperties properties) {
        this.teamService = teamService;
        this.evaluationService = evaluationService;
        this.properties = properties;
    }

    @PostMapping("/chat")
    public AgentTeamResponse chat(@RequestBody ChatRequest request) {
        return teamService.chat(request);
    }

    @GetMapping("/agents")
    public Collection<SpecialistAgent> agents() {
        return teamService.agents();
    }

    @GetMapping("/evaluate")
    public Map<String, Object> evaluate() {
        return evaluationService.evaluate();
    }

    @GetMapping("/runtime")
    public Map<String, Object> runtime() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("storage", properties.getStorage());
        result.put("contextStore", properties.getContextStore());
        result.put("database", "mysql".equalsIgnoreCase(properties.getStorage()) ? "agent_gateway" : "memory");
        return result;
    }
}
