package com.example.integrations.config;

import com.example.integrations.sso.SsoAuthorizeRequest;
import com.example.integrations.sso.SsoService;
import com.example.integrations.sso.SsoSignUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 仅用于本地演示：浏览器不接触 appSecret，由服务端生成签名。
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final IntegrationProperties properties;
    private final SsoService ssoService;

    public DemoController(IntegrationProperties properties, SsoService ssoService) {
        this.properties = properties;
        this.ssoService = ssoService;
    }

    @PostMapping("/sso/authorize")
    public Map<String, Object> ssoAuthorize(@RequestBody Map<String, String> body) {
        SsoAuthorizeRequest request = new SsoAuthorizeRequest();
        request.setAppId("ys-demo");
        request.setUserId(defaultValue(body.get("userId"), "ys-user-001"));
        request.setTargetType(defaultValue(body.get("targetType"), "receiveInStock"));
        request.setTargetId(defaultValue(body.get("targetId"), "RIS-1001"));
        request.setTimestamp(String.valueOf(System.currentTimeMillis()));
        request.setNonce(UUID.randomUUID().toString().replace("-", ""));
        Map<String, String> fields = new LinkedHashMap<String, String>(request.signFields());
        request.setSign(SsoSignUtil.sign(fields, properties.getSsoAppSecret()));
        return ssoService.authorize(request);
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
