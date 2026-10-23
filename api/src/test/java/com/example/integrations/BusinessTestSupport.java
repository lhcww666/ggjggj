package com.example.integrations;

import com.example.integrations.auth.AuthSession;
import com.example.integrations.config.IntegrationProperties;
import com.example.integrations.operation.OperationLogService;
import com.example.integrations.repository.MemoryDemoRepository;

import java.util.Arrays;
import java.util.List;

public abstract class BusinessTestSupport {

    protected final MemoryDemoRepository repository = new MemoryDemoRepository();
    protected final OperationLogService logService = new OperationLogService(repository);

    protected IntegrationProperties properties() {
        IntegrationProperties properties = new IntegrationProperties();
        properties.setSsoBaseUrl("http://localhost:9000/business");
        properties.setSsoAppSecret("test-sso-secret");
        properties.setSsoCodeTtlSeconds(60);
        properties.setSfCheckWord("test-sf-check-word");
        properties.setSfCustomerCode("TEST_CUSTOMER");
        return properties;
    }

    protected AuthSession session(String role) {
        List<String> permissions;
        if ("ADMIN".equals(role)) {
            permissions = Arrays.asList("SSO", "BARCODE", "BILLTRACE", "SF", "RAG", "RAG_ADMIN", "AUDIT", "ADMIN");
        } else if ("OPERATOR".equals(role)) {
            permissions = Arrays.asList("SSO", "BARCODE", "BILLTRACE", "SF", "RAG");
        } else {
            permissions = Arrays.asList("SSO", "BARCODE", "BILLTRACE", "RAG");
        }
        return new AuthSession("token-" + role.toLowerCase(), role.toLowerCase(), role, role, permissions, "TEST");
    }
}
