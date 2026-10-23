package com.example.integrations.rag;

import com.example.integrations.auth.AuthSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;
    private final ObjectMapper mapper;
    private final DocumentTextExtractor textExtractor;
    private final RagLlmProperties llmProperties;

    public RagController(RagService ragService, ObjectMapper mapper,
                         DocumentTextExtractor textExtractor, RagLlmProperties llmProperties) {
        this.ragService = ragService;
        this.mapper = mapper;
        this.textExtractor = textExtractor;
        this.llmProperties = llmProperties;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("retrieval", "keyword+ngram+hash-embedding");
        data.put("vectorStore", ragService.vectorStoreMode());
        data.put("answerMode", llmProperties.isEnabled() ? "llm-with-template-fallback" : "template");
        data.put("llmEnabled", llmProperties.isEnabled());
        return success(data);
    }

    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody AskRequest request,
                                   @RequestAttribute("authSession") AuthSession session) {
        Map<String, Object> data = ragService.ask(request.getQuery(), session, request.getTopK());
        return success(data);
    }

    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody AskRequest request,
                             @RequestAttribute("authSession") AuthSession session) {
        SseEmitter emitter = new SseEmitter(60_000L);
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> result = ragService.ask(request.getQuery(), session, request.getTopK());
                emitter.send(SseEmitter.event().name("sources")
                        .data(mapper.writeValueAsString(result.get("sources")), MediaType.APPLICATION_JSON));
                emitter.send(SseEmitter.event().name("message")
                        .data(mapper.writeValueAsString(result.get("answer")), MediaType.APPLICATION_JSON));
                emitter.send(SseEmitter.event().name("done").data("{}", MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception exception) {
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    @GetMapping("/documents")
    public Map<String, Object> documents(@RequestAttribute("authSession") AuthSession session) {
        return success(ragService.list(session));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam String title,
            @RequestParam(defaultValue = "PUBLIC") String visibility,
            @RequestPart("file") MultipartFile file,
            @RequestAttribute("authSession") AuthSession session) throws Exception {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!(name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".docx") || name.endsWith(".pdf"))) {
            return ResponseEntity.badRequest().body(error("UNSUPPORTED_FILE", "演示版支持 txt/md/docx/pdf 文件"));
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(error("FILE_TOO_LARGE", "文件不能超过 10MB"));
        }
        String content = textExtractor.extract(file);
        return ResponseEntity.ok(success(ragService.add(title, content, visibility, session)));
    }

    @PostMapping("/documents/text")
    public Map<String, Object> addText(@RequestBody DocumentRequest request,
                                       @RequestAttribute("authSession") AuthSession session) {
        return success(ragService.add(request.getTitle(), request.getContent(),
                request.getVisibility(), session));
    }

    @DeleteMapping("/documents/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        return success(ragService.delete(id));
    }

    @GetMapping("/evaluate")
    public Map<String, Object> evaluate(@RequestAttribute("authSession") AuthSession session) {
        return success(ragService.evaluate(session));
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", true);
        result.put("code", 200);
        result.put("message", "成功");
        result.put("data", data);
        return result;
    }

    private Map<String, Object> error(String code, String message) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", false);
        result.put("code", code);
        result.put("message", message);
        return result;
    }

    public static class AskRequest {
        private String query;
        private int topK = 5;
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
    }

    public static class DocumentRequest {
        private String title;
        private String content;
        private String visibility = "PUBLIC";
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getVisibility() { return visibility; }
        public void setVisibility(String visibility) { this.visibility = visibility; }
    }
}
