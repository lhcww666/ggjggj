package com.example.integrations.sso;

public class SsoTarget {

    private final String type;
    private final String path;
    private final String id;

    public SsoTarget(String type, String path, String id) {
        this.type = type;
        this.path = path;
        this.id = id;
    }

    public String getType() { return type; }
    public String getPath() { return path; }
    public String getId() { return id; }
}
