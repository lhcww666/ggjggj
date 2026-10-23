package com.example.integrations.auth;

import com.example.integrations.operation.OperationLogService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AuthService {

    private final OperationLogService logService;
    private final ConcurrentMap<String, AuthSession> sessions = new ConcurrentHashMap<String, AuthSession>();
    private final Map<String, DemoUser> users = new LinkedHashMap<String, DemoUser>();

    public AuthService(OperationLogService logService) {
        this.logService = logService;
        users.put("admin", new DemoUser("admin", "admin123", "管理员", "ADMIN"));
        users.put("operator", new DemoUser("operator", "operator123", "业务员", "OPERATOR"));
        users.put("viewer", new DemoUser("viewer", "viewer123", "查看者", "VIEWER"));
    }

    public AuthSession login(String username, String password) {
        DemoUser user = users.get(username);
        if (user == null || !user.password.equals(password)) {
            logService.record(username == null ? "anonymous" : username,
                    "LOGIN", "用户名或密码错误", false);
            throw new IllegalArgumentException("用户名或密码错误");
        }
        AuthSession session = newSession(user.userId, user.displayName, user.role, "PASSWORD");
        logService.record(user.userId, "LOGIN", "本地账号登录成功", true);
        return session;
    }

    public AuthSession issueSsoToken(String localUserId) {
        String role = "local-user-1002".equals(localUserId) ? "VIEWER" : "OPERATOR";
        String displayName = "local-user-1002".equals(localUserId) ? "SSO 查看者" : "SSO 业务员";
        AuthSession session = newSession(localUserId, displayName, role, "SSO");
        logService.record(localUserId, "SSO_LOGIN", "SSO 登录成功", true);
        return session;
    }

    public AuthSession resolve(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        return sessions.get(token);
    }

    public void logout(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public List<DemoUser> demoUsers() {
        return Arrays.asList(
                new DemoUser("admin", "admin123", "管理员", "ADMIN"),
                new DemoUser("operator", "operator123", "业务员", "OPERATOR"),
                new DemoUser("viewer", "viewer123", "查看者", "VIEWER")
        );
    }

    private AuthSession newSession(String userId, String displayName, String role, String source) {
        String token = "demo-" + UUID.randomUUID().toString().replace("-", "");
        AuthSession session = new AuthSession(token, userId, displayName, role,
                permissions(role), source);
        sessions.put(token, session);
        return session;
    }

    private List<String> permissions(String role) {
        if ("ADMIN".equals(role)) {
            return Arrays.asList("SSO", "BARCODE", "BILLTRACE", "SF", "RAG", "RAG_ADMIN", "AUDIT", "ADMIN");
        }
        if ("OPERATOR".equals(role)) {
            return Arrays.asList("SSO", "BARCODE", "BILLTRACE", "SF", "RAG");
        }
        return Arrays.asList("SSO", "BARCODE", "BILLTRACE", "RAG");
    }

    public static final class DemoUser {
        private final String userId;
        private final String password;
        private final String displayName;
        private final String role;

        public DemoUser(String userId, String password, String displayName, String role) {
            this.userId = userId;
            this.password = password;
            this.displayName = displayName;
            this.role = role;
        }

        public String getUserId() { return userId; }
        public String getDisplayName() { return displayName; }
        public String getRole() { return role; }
    }
}
