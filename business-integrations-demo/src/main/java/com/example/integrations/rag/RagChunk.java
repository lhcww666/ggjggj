package com.example.integrations.rag;

public class RagChunk {

    private final String documentId;
    private final String title;
    private final String content;
    private final String visibility;
    private final double[] embedding;

    public RagChunk(String documentId, String title, String content, String visibility, double[] embedding) {
        this.documentId = documentId;
        this.title = title;
        this.content = content;
        this.visibility = visibility;
        this.embedding = embedding;
    }

    public String getDocumentId() { return documentId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getVisibility() { return visibility; }
    public double[] getEmbedding() { return embedding; }
}
