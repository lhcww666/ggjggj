package com.example.integrations.sso;

import com.example.integrations.BusinessTestSupport;
import com.example.integrations.auth.AuthService;
import com.example.integrations.config.IntegrationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SsoServiceTest extends BusinessTestSupport {

    private IntegrationProperties properties;
    private SsoService ssoService;

    @BeforeEach
    void setUp() {
        properties = properties();
        AuthService authService = new AuthService(logService);
        ssoService = new SsoService(properties, new SsoTargetRegistry(),
                new MemorySsoStateStore(), repository, authService, logService);
    }

    @Test
    void signedRequestCanExchangeOneTimeCode() {
        Map<String, Object> response = ssoService.authorize(request("ys-user-001", "receiveInStock"));

        assertThat(response.get("success")).isEqualTo(true);
        Map<String, Object> data = data(response);
        assertThat((String) data.get("code")).isNotBlank();
        assertThat((String) data.get("loginUrl")).startsWith("http://localhost:9000/business/#/sso/login");

        Map<String, Object> exchange = ssoService.exchange((String) data.get("code"));
        assertThat(exchange.get("success")).isEqualTo(true);
        assertThat(((SsoTarget) data(exchange).get("target")).getType()).isEqualTo("receiveInStock");
    }

    @Test
    void nonceCannotBeReplayed() {
        SsoAuthorizeRequest request = request("ys-user-001", "receiveInStock");

        assertThat(ssoService.authorize(request).get("success")).isEqualTo(true);
        assertThat(ssoService.authorize(request).get("code")).isEqualTo(1003);
    }

    @Test
    void invalidSignatureIsRejected() {
        SsoAuthorizeRequest request = request("ys-user-001", "receiveInStock");
        request.setSign("invalid");

        assertThat(ssoService.authorize(request).get("code")).isEqualTo(1001);
    }

    @Test
    void staleTimestampIsRejected() {
        SsoAuthorizeRequest request = request("ys-user-001", "receiveInStock");
        request.setTimestamp(String.valueOf(System.currentTimeMillis() - 10 * 60 * 1000L));
        request.setSign(SsoSignUtil.sign(request.signFields(), properties.getSsoAppSecret()));

        assertThat(ssoService.authorize(request).get("code")).isEqualTo(1002);
    }

    @Test
    void unknownUserAndTargetAreRejected() {
        assertThat(ssoService.authorize(request("unknown-user", "receiveInStock")).get("code")).isEqualTo(1006);
        assertThat(ssoService.authorize(request("ys-user-001", "unknown-target")).get("code")).isEqualTo(1007);
    }

    @Test
    void codeCanOnlyBeExchangedOnce() {
        Map<String, Object> response = ssoService.authorize(request("ys-user-001", "receiveInStock"));
        String code = (String) data(response).get("code");

        assertThat(ssoService.exchange(code).get("success")).isEqualTo(true);
        assertThat(ssoService.exchange(code).get("code")).isEqualTo(1009);
    }

    private SsoAuthorizeRequest request(String userId, String targetType) {
        SsoAuthorizeRequest request = new SsoAuthorizeRequest();
        request.setAppId("ys-demo");
        request.setUserId(userId);
        request.setTargetType(targetType);
        request.setTargetId("RIS-1001");
        request.setTimestamp(String.valueOf(System.currentTimeMillis()));
        request.setNonce(UUID.randomUUID().toString().replace("-", ""));
        request.setSign(SsoSignUtil.sign(request.signFields(), properties.getSsoAppSecret()));
        return request;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> response) {
        return (Map<String, Object>) response.get("data");
    }
}
