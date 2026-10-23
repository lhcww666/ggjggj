package com.example.agentdemo.controller;

import com.example.agentdemo.model.ChatRequest;
import com.example.agentdemo.model.ChatResponse;
import com.example.agentdemo.service.AgentService;
import com.example.agentdemo.tool.Tool;
import com.example.agentdemo.tool.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;
    private final ToolRegistry registry;
    private final ObjectMapper mapper;

    public AgentController(AgentService agentService, ToolRegistry registry, ObjectMapper mapper) {
        this.agentService = agentService;
        this.registry = registry;
        this.mapper = mapper;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return agentService.chat(request);
    }

    /**
     * 学习版 SSE：先把完整结果算出来，再按事件发送。
     * 真正接 Dify 时，应该在 Dify 流式回调中逐段发送 message 事件。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(60_000L);
        CompletableFuture.runAsync(() -> {
            try {
                ChatResponse response = agentService.chat(request);
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(mapper.writeValueAsString(response.getAnswer()), MediaType.APPLICATION_JSON));
                emitter.send(SseEmitter.event()
                        .name("metadata")
                        .data(mapper.writeValueAsString(response), MediaType.APPLICATION_JSON));
                emitter.send(SseEmitter.event().name("done").data("{}", MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception exception) {
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"message\":\"处理失败\"}", MediaType.APPLICATION_JSON));
                } catch (Exception ignored) {
                    // 连接可能已经关闭
                }
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    @GetMapping("/tools")
    public Map<String, Object> tools() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Tool tool : registry.all().values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", tool.name());
            item.put("description", tool.description());
            item.put("inputSchema", tool.inputSchema());
            result.put(tool.name(), item);
        }
        return result;
    }
}
