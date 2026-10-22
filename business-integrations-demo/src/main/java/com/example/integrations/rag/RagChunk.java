package com.example.integrations.rag;

public class RagChunk {

    private final String documentId;
    private final String title;
    private final String content;
    private final String visibility;

    public RagChunk(String documentId, String title, String content, String visibility) {
        this.documentId = documentId;
        this.title = title;
        this.content = content;
        this.visibility = visibility;
    }

    public String getDocumentId() { return documentId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getVisibility() { return visibility; }
}
