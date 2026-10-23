package com.example.agentdemo.controller;

import com.example.agentdemo.model.ChatRequest;
import com.example.agentdemo.team.AgentTeamResponse;
import com.example.agentdemo.team.AgentTeamService;
import com.example.agentdemo.team.SpecialistAgent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/api/agent/team")
public class AgentTeamController {

    private final AgentTeamService teamService;

    public AgentTeamController(AgentTeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping("/chat")
    public AgentTeamResponse chat(@RequestBody ChatRequest request) {
        return teamService.chat(request);
    }

    @GetMapping("/agents")
    public Collection<SpecialistAgent> agents() {
        return teamService.agents();
    }
}
