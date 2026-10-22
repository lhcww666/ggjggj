package com.example.integrations.sso;

import com.example.integrations.config.IntegrationProperties;
import com.example.integrations.auth.AuthService;
import com.example.integrations.auth.AuthSession;
import com.example.integrations.operation.OperationLogService;
import com.example.integrations.store.DemoRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SsoService {

    private static final long MAX_CLOCK_SKEW_MILLIS = 5 * 60 * 1000L;

    private final IntegrationProperties properties;
    private final SsoTargetRegistry targetRegistry;
    private final SsoStateStore stateStore;
    private final DemoRepository repository;
    private final AuthService authService;
    private final OperationLogService logService;

    public SsoService(IntegrationProperties properties, SsoTargetRegistry targetRegistry,
                      SsoStateStore stateStore, DemoRepository repository,
                      AuthService authService, OperationLogService logService) {
        this.properties = properties;
        this.targetRegistry = targetRegistry;
        this.stateStore = stateStore;
        this.repository = repository;
        this.authService = authService;
        this.logService = logService;
    }

    public Map<String, Object> authorize(SsoAuthorizeRequest request) {
        if (isBlank(request.getAppId()) || isBlank(request.getUserId())
                || isBlank(request.getTargetType()) || isBlank(request.getTargetId())
                || isBlank(request.getTimestamp()) || isBlank(request.getNonce())
                || isBlank(request.getSign())) {
            return error(1000, "必填参数缺失");
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(request.getTimestamp());
        } catch (NumberFormatException exception) {
            return error(1002, "时间戳格式错误");
        }
        if (Math.abs(System.currentTimeMillis() - timestamp) > MAX_CLOCK_SKEW_MILLIS) {
            return error(1002, "时间戳超期");
        }

        String expected = SsoSignUtil.sign(request.signFields(), properties.getSsoAppSecret());
        if (!SsoSignUtil.safeEquals(expected, request.getSign())) {
            return error(1001, "签名校验失败");
        }
        if (!stateStore.markNonce(request.getNonce(), properties.getSsoCodeTtlSeconds())) {
            return error(1003, "重复请求");
        }

        String localUserId = repository.findLocalUserId(request.getUserId());
        if (localUserId == null) {
            return error(1006, "用户未映射");
        }

        SsoTarget target = targetRegistry.resolve(request.getTargetType(), request.getTargetId());
        if (target == null) {
            return error(1007, "目标单据类型未授权");
        }

        String code = UUID.randomUUID().toString().replace("-", "");
        long expiresAt = System.currentTimeMillis() + properties.getSsoCodeTtlSeconds() * 1000L;
        stateStore.saveCode(code, new SsoSession(localUserId, target, expiresAt),
                properties.getSsoCodeTtlSeconds());
        logService.record(localUserId, "SSO_AUTHORIZE", "换取一次性 code", true);

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("code", code);
        data.put("loginUrl", properties.getSsoBaseUrl() + "/#/sso/login?code=" + code);
        data.put("expiresIn", properties.getSsoCodeTtlSeconds());
        return success(data);
    }

    public Map<String, Object> exchange(String code) {
        SsoSession session = stateStore.takeCode(code);
        if (session == null || session.isExpired()) {
            return error(1009, "临时 code 无效或已过期");
        }
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        AuthSession authSession = authService.issueSsoToken(session.getLocalUserId());
        data.put("token", authSession.getToken());
        Map<String, Object> user = new LinkedHashMap<String, Object>();
        user.put("id", authSession.getUserId());
        user.put("name", authSession.getDisplayName());
        user.put("displayName", authSession.getDisplayName());
        user.put("role", authSession.getRole());
        user.put("permissions", authSession.getPermissions());
        data.put("user", user);
        data.put("target", session.getTarget());
        logService.record(session.getLocalUserId(), "SSO_EXCHANGE", "code 兑换登录态", true);
        return success(data);
    }

    private Map<String, Object> success(Object data) {
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
        result.put("data", null);
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
