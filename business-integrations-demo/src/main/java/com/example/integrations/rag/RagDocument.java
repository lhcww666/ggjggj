package com.example.integrations.rag;

public class RagDocument {

    private final String id;
    private final String title;
    private final String content;
    private final String visibility;
    private final String owner;

    public RagDocument(String id, String title, String content, String visibility, String owner) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.visibility = visibility;
        this.owner = owner;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getVisibility() { return visibility; }
    public String getOwner() { return owner; }
}
