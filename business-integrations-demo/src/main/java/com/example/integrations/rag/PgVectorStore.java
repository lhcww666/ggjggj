package com.example.integrations.rag;

import com.example.integrations.auth.AuthSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "rag.vector-store", havingValue = "pgvector")
public class PgVectorStore {

    private final JdbcTemplate jdbc;
    private final int dimension;

    public PgVectorStore(JdbcTemplate pgVectorJdbcTemplate, RagPgVectorProperties properties) {
        this.jdbc = pgVectorJdbcTemplate;
        this.dimension = Math.max(8, Math.min(4096, properties.getDimension()));
    }

    @PostConstruct
    public void ensureSchema() {
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbc.execute("CREATE TABLE IF NOT EXISTS rag_chunk (" +
                "id VARCHAR(128) PRIMARY KEY, document_id VARCHAR(64) NOT NULL, " +
                "title VARCHAR(256) NOT NULL, content TEXT NOT NULL, visibility VARCHAR(16) NOT NULL, " +
                "embedding vector(" + dimension + ") NOT NULL)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunk_embedding " +
                "ON rag_chunk USING hnsw (embedding vector_cosine_ops)");
    }

    public void addAll(List<RagChunk> chunks) {
        for (RagChunk chunk : chunks) {
            jdbc.update("INSERT INTO rag_chunk (id, document_id, title, content, visibility, embedding) " +
                            "VALUES (?, ?, ?, ?, ?, CAST(? AS vector))",
                    chunk.getDocumentId() + "-" + Math.abs(chunk.getContent().hashCode()),
                    chunk.getDocumentId(), chunk.getTitle(), chunk.getContent(),
                    chunk.getVisibility(), vectorText(chunk.getEmbedding()));
        }
    }

    public void deleteByDocument(String documentId) {
        jdbc.update("DELETE FROM rag_chunk WHERE document_id = ?", documentId);
    }

    public List<Map<String, Object>> search(double[] queryVector, AuthSession session, int topK) {
        String permission;
        if (session != null && "ADMIN".equals(session.getRole())) {
            permission = "'PUBLIC','OPERATOR','ADMIN'";
        } else if (session != null && "OPERATOR".equals(session.getRole())) {
            permission = "'PUBLIC','OPERATOR'";
        } else {
            permission = "'PUBLIC'";
        }
        String vector = vectorText(queryVector);
        return jdbc.query(
                "SELECT document_id, title, content, visibility, " +
                        "1 - (embedding <=> CAST(? AS vector)) AS vector_score " +
                        "FROM rag_chunk WHERE visibility IN (" + permission + ") " +
                        "ORDER BY embedding <=> CAST(? AS vector) LIMIT ?",
                (rs, rowNum) -> {
                    Map<String, Object> source = new LinkedHashMap<String, Object>();
                    source.put("documentId", rs.getString("document_id"));
                    source.put("title", rs.getString("title"));
                    source.put("snippet", rs.getString("content"));
                    source.put("visibility", rs.getString("visibility"));
                    double score = rs.getDouble("vector_score");
                    source.put("score", round(score));
                    source.put("vectorScore", round(score));
                    source.put("keywordScore", 0);
                    return source;
                }, vector, vector, Math.max(1, topK));
    }

    private String vectorText(double[] vector) {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < dimension; i++) {
            if (i > 0) result.append(',');
            result.append(i < vector.length ? vector[i] : 0.0);
        }
        return result.append(']').toString();
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
