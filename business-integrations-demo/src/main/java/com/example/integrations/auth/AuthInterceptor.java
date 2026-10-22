package com.example.integrations.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final ObjectMapper mapper;

    public AuthInterceptor(AuthService authService, ObjectMapper mapper) {
        this.authService = authService;
        this.mapper = mapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();
        if (isPublic(path) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        AuthSession session = authService.resolve(AuthController.bearer(request.getHeader("Authorization")));
        if (session == null) {
            write(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录");
            return false;
        }
        String required = requiredPermission(path);
        if (required != null && !session.getPermissions().contains(required)) {
            write(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "当前角色无权访问: " + required);
            return false;
        }
        request.setAttribute("authSession", session);
        return true;
    }

    private boolean isPublic(String path) {
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/sso/")
                || path.startsWith("/api/demo/sso/")
                || path.startsWith("/api/v1/barcodeFlowRecord/trace/");
    }

    private String requiredPermission(String path) {
        if (path.startsWith("/api/sf/")) return "SF";
        if (path.startsWith("/api/v1/billtrace")) return "BILLTRACE";
        if (path.startsWith("/api/admin/")) return "AUDIT";
        return null;
    }

    private void write(HttpServletResponse response, HttpStatus status, String code, String message)
            throws Exception {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", false);
        result.put("code", code);
        result.put("message", message);
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(mapper.writeValueAsString(result));
    }
}
