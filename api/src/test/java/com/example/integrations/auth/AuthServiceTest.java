package com.example.integrations.auth;

import com.example.integrations.BusinessTestSupport;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest extends BusinessTestSupport {

    private final AuthService authService = new AuthService(logService);

    @Test
    void adminReceivesAllManagementPermissions() {
        AuthSession session = authService.login("admin", "admin123");

        assertThat(session.getRole()).isEqualTo("ADMIN");
        assertThat(session.getPermissions()).contains("SF", "AUDIT", "RAG_ADMIN", "ADMIN");
    }

    @Test
    void operatorCannotAccessAuditOrAdmin() {
        AuthSession session = authService.login("operator", "operator123");

        assertThat(session.getPermissions()).contains("SF", "RAG");
        assertThat(session.getPermissions()).doesNotContain("AUDIT", "RAG_ADMIN", "ADMIN");
    }

    @Test
    void viewerCannotUseSfOrRagAdmin() {
        AuthSession session = authService.login("viewer", "viewer123");

        assertThat(session.getPermissions()).contains("RAG");
        assertThat(session.getPermissions()).doesNotContain("SF", "RAG_ADMIN");
    }

    @Test
    void invalidCredentialsAreRejectedAndLogged() {
        assertThatThrownBy(() -> authService.login("admin", "wrong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("用户名或密码错误");

        assertThat(logService.list(10))
                .anySatisfy(item -> {
                    assertThat(item.get("action")).isEqualTo("LOGIN");
                    assertThat(item.get("success")).isEqualTo(false);
                });
    }

    @Test
    void logoutInvalidatesToken() {
        AuthSession session = authService.login("viewer", "viewer123");
        assertThat(authService.resolve(session.getToken())).isNotNull();

        authService.logout(session.getToken());

        assertThat(authService.resolve(session.getToken())).isNull();
    }
}
