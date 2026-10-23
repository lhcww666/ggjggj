package com.example.integrations.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        AuthSession session = authService.login(request.getUsername(), request.getPassword());
        return ok(session);
    }

    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthSession session = authService.resolve(bearer(authorization));
        if (session == null) {
            return error(401, "登录状态已失效");
        }
        return ok(session);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(bearer(authorization));
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", true);
        result.put("message", "已退出登录");
        return result;
    }

    private Map<String, Object> ok(AuthSession session) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("token", session.getToken());
        data.put("user", session);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", true);
        result.put("code", 200);
        result.put("message", "成功");
        result.put("data", data);
        return result;
    }

    private Map<String, Object> error(int code, String message) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", false);
        result.put("code", code);
        result.put("message", message);
        return result;
    }

    static String bearer(String header) {
        if (header == null || header.trim().isEmpty()) {
            return null;
        }
        return header.startsWith("Bearer ") ? header.substring(7) : header;
    }

    public static class LoginRequest {
        private String username;
        private String password;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
