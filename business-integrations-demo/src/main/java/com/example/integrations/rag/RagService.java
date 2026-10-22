package com.example.integrations.rag;

import com.example.integrations.auth.AuthSession;
import com.example.integrations.operation.OperationLogService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RagService {

    private static final int CHUNK_SIZE = 280;
    private static final int CHUNK_OVERLAP = 40;

    private final List<RagDocument> documents = new CopyOnWriteArrayList<RagDocument>();
    private final List<RagChunk> chunks = new CopyOnWriteArrayList<RagChunk>();
    private final OperationLogService logService;
    private final EmbeddingModel embeddingModel;
    private final AnswerGenerator answerGenerator;

    public RagService(OperationLogService logService, EmbeddingModel embeddingModel,
                      AnswerGenerator answerGenerator) {
        this.logService = logService;
        this.embeddingModel = embeddingModel;
        this.answerGenerator = answerGenerator;
        seed(
                "采购下单流程",
                "采购订单先校验供应商编码、物料编码、数量和含税单价。通过后只生成采购草稿，"
                        + "必须由用户确认后才能创建正式订单。相同 requestId 或草稿只能执行一次，"
                        + "重复请求应返回已有订单或提示已使用，防止重复下单。",
                "PUBLIC", "system");
        seed(
                "入库质检规范",
                "采购入库前需要核对到货单、供应商、物料批次和数量。质检不合格时进入异常处理，"
                        + "不能直接审核入库。入库审核后应回写条码状态和库存流水。",
                "OPERATOR", "system");
        seed(
                "顺丰物流异常处理",
                "顺丰下单请求成功只代表网关层成功，还要校验业务 success。如果顺丰已经成功但本地运单号回写失败，"
                        + "不能依赖本地事务回滚外部订单，应保存调用状态并通过对账、查单和重试补偿。",
                "OPERATOR", "system");
        seed(
                "SSO 对接说明",
                "第三方后端使用 appSecret 对参数签名，换取 60 秒有效的一次性 code。"
                        + "浏览器用 code 换取登录态，Token 不进入 URL。timestamp 和 nonce 用于防止重放，"
                        + "同一个 code 只能兑换一次。",
                "PUBLIC", "system");
        seed(
                "管理员权限规范",
                "管理员可以查看操作日志、配置用户角色和审计关键动作。普通业务员只能访问业务菜单，"
                        + "查看者只能查询公开知识。前端菜单隐藏不能代替后端接口权限校验。",
                "ADMIN", "system");
    }

    public List<RagDocument> list(AuthSession session) {
        List<RagDocument> result = new ArrayList<RagDocument>();
        for (RagDocument document : documents) {
            if (visible(document.getVisibility(), session)) {
                result.add(document);
            }
        }
        return result;
    }

    public RagDocument add(String title, String content, String visibility, AuthSession session) {
        String normalizedTitle = title == null ? "" : title.trim();
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedTitle.isEmpty() || normalizedContent.isEmpty()) {
            throw new IllegalArgumentException("标题和内容不能为空");
        }
        String normalizedVisibility = normalizeVisibility(visibility);
        RagDocument document = new RagDocument(UUID.randomUUID().toString(), normalizedTitle,
                normalizedContent, normalizedVisibility, session == null ? "system" : session.getUserId());
        synchronized (this) {
            documents.add(document);
            chunks.addAll(split(document));
        }
        logService.record(document.getOwner(), "RAG_DOCUMENT_ADD", normalizedTitle, true);
        return document;
    }

    public boolean delete(String id) {
        synchronized (this) {
            boolean removed = documents.removeIf(document -> document.getId().equals(id));
            if (removed) {
                chunks.removeIf(chunk -> chunk.getDocumentId().equals(id));
            }
            return removed;
        }
    }

    public Map<String, Object> ask(String query, AuthSession session, int topK) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        List<Map<String, Object>> hits = search(normalizedQuery, session, topK);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("query", normalizedQuery);
        result.put("sources", hits);
        result.put("answer", answerGenerator.generate(normalizedQuery, hits));
        result.put("generatedBy", "hybrid-local-template");
        logService.record(session == null ? "anonymous" : session.getUserId(),
                "RAG_ASK", normalizedQuery, true);
        return result;
    }

    public Map<String, Object> evaluate(AuthSession session) {
        List<Map<String, Object>> cases = new ArrayList<Map<String, Object>>();
        int hits = 0;
        String[][] tests = {
                {"采购单重复创建怎么办", "采购下单流程"},
                {"顺丰成功但本地写回失败", "顺丰物流异常处理"},
                {"SSO code 能不能重复使用", "SSO 对接说明"},
                {"管理员怎么查操作日志", "管理员权限规范"},
                {"入库质检不合格怎么处理", "入库质检规范"}
        };
        for (String[] test : tests) {
            List<Map<String, Object>> sources = search(test[0], session, 3);
            boolean hit = false;
            for (Map<String, Object> source : sources) {
                if (test[1].equals(source.get("title"))) {
                    hit = true;
                    break;
                }
            }
            if (hit) hits++;
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("query", test[0]);
            item.put("expected", test[1]);
            item.put("hit", hit);
            item.put("topTitles", titles(sources));
            cases.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", tests.length);
        result.put("hits", hits);
        result.put("hitRate", tests.length == 0 ? 0 : (double) hits / tests.length);
        result.put("cases", cases);
        return result;
    }

    private List<Map<String, Object>> search(String query, AuthSession session, int topK) {
        List<RagChunk> visible = new ArrayList<RagChunk>();
        for (RagChunk chunk : chunks) {
            if (visible(chunk.getVisibility(), session)) {
                visible.add(chunk);
            }
        }
        if (visible.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> queryTokens = TextTokenizer.tokens(query);
        double[] queryVector = embeddingModel.embed(query);
        Map<String, Integer> documentFrequency = new HashMap<String, Integer>();
        for (RagChunk chunk : visible) {
            for (String token : TextTokenizer.tokens(chunk.getContent())) {
                documentFrequency.merge(token, 1, Integer::sum);
            }
        }
        List<ScoredChunk> scored = new ArrayList<ScoredChunk>();
        for (RagChunk chunk : visible) {
            List<String> chunkTokens = TextTokenizer.tokens(chunk.getContent());
            Map<String, Integer> frequencies = new HashMap<String, Integer>();
            for (String token : chunkTokens) {
                frequencies.merge(token, 1, Integer::sum);
            }
            double keywordScore = 0;
            for (String token : queryTokens) {
                Integer tf = frequencies.get(token);
                if (tf == null) continue;
                int df = documentFrequency.getOrDefault(token, 0);
                double idf = Math.log((visible.size() + 1.0) / (df + 1.0)) + 1.0;
                keywordScore += tf * idf;
            }
            double vectorScore = cosine(queryVector, chunk.getEmbedding());
            double score = keywordScore + vectorScore * 2.0;
            if (score > 0) {
                scored.add(new ScoredChunk(chunk, keywordScore, vectorScore, score));
            }
        }
        scored.sort(Comparator.comparingDouble(ScoredChunk::getScore).reversed());
        int size = Math.min(topK <= 0 ? 5 : topK, scored.size());
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < size; i++) {
            ScoredChunk item = scored.get(i);
            String dedupKey = item.chunk.getDocumentId() + item.chunk.getContent();
            if (!seen.add(dedupKey)) continue;
            Map<String, Object> source = new LinkedHashMap<String, Object>();
            source.put("documentId", item.chunk.getDocumentId());
            source.put("title", item.chunk.getTitle());
            source.put("score", Math.round(item.score * 1000.0) / 1000.0);
            source.put("keywordScore", Math.round(item.keywordScore * 1000.0) / 1000.0);
            source.put("vectorScore", Math.round(item.vectorScore * 1000.0) / 1000.0);
            source.put("snippet", item.chunk.getContent());
            source.put("visibility", item.chunk.getVisibility());
            result.add(source);
        }
        return result;
    }

    private void seed(String title, String content, String visibility, String owner) {
        RagDocument document = new RagDocument(UUID.randomUUID().toString(), title, content, visibility, owner);
        documents.add(document);
        chunks.addAll(split(document));
    }

    private List<RagChunk> split(RagDocument document) {
        List<RagChunk> result = new ArrayList<RagChunk>();
        String content = document.getContent();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(content.length(), start + CHUNK_SIZE);
            String part = content.substring(start, end).trim();
            if (!part.isEmpty()) {
                result.add(new RagChunk(document.getId(), document.getTitle(), part,
                        document.getVisibility(), embeddingModel.embed(part)));
            }
            if (end == content.length()) break;
            start = Math.max(start + 1, end - CHUNK_OVERLAP);
        }
        return result;
    }

    private double cosine(double[] left, double[] right) {
        if (left == null || right == null) return 0;
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        int length = Math.min(left.length, right.length);
        for (int i = 0; i < length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) return 0;
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private boolean visible(String visibility, AuthSession session) {
        if ("PUBLIC".equals(visibility)) return true;
        if (session == null) return false;
        if ("ADMIN".equals(visibility)) return "ADMIN".equals(session.getRole());
        return "ADMIN".equals(session.getRole()) || "OPERATOR".equals(session.getRole());
    }

    private String normalizeVisibility(String visibility) {
        String value = visibility == null ? "PUBLIC" : visibility.toUpperCase();
        if (!"PUBLIC".equals(value) && !"OPERATOR".equals(value) && !"ADMIN".equals(value)) {
            return "PUBLIC";
        }
        return value;
    }

    private List<String> titles(List<Map<String, Object>> sources) {
        List<String> result = new ArrayList<String>();
        for (Map<String, Object> source : sources) {
            result.add(String.valueOf(source.get("title")));
        }
        return result;
    }

    private static final class ScoredChunk {
        private final RagChunk chunk;
        private final double keywordScore;
        private final double vectorScore;
        private final double score;

        private ScoredChunk(RagChunk chunk, double keywordScore, double vectorScore, double score) {
            this.chunk = chunk;
            this.keywordScore = keywordScore;
            this.vectorScore = vectorScore;
            this.score = score;
        }

        private double getScore() {
            return score;
        }
    }
}
