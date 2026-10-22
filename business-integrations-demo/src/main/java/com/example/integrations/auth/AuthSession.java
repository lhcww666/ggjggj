package com.example.integrations.auth;

import java.util.List;

public class AuthSession {

    private final String token;
    private final String userId;
    private final String displayName;
    private final String role;
    private final List<String> permissions;
    private final String source;

    public AuthSession(String token, String userId, String displayName,
                       String role, List<String> permissions, String source) {
        this.token = token;
        this.userId = userId;
        this.displayName = displayName;
        this.role = role;
        this.permissions = permissions;
        this.source = source;
    }

    public String getToken() { return token; }
    public String getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public List<String> getPermissions() { return permissions; }
    public String getSource() { return source; }
}
