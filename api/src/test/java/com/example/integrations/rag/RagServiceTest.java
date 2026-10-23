package com.example.integrations.rag;

import com.example.integrations.BusinessTestSupport;
import com.example.integrations.auth.AuthSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagServiceTest extends BusinessTestSupport {

    private RagService ragService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<PgVectorStore> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        ragService = new RagService(logService, new HashingEmbeddingModel(),
                new TemplateAnswerGenerator(), provider);
    }

    @Test
    void publicQuestionReturnsCitedSources() {
        Map<String, Object> result = ragService.ask("采购单重复创建怎么办", session("VIEWER"), 3);

        assertThat(result.get("answer").toString()).contains("采购下单流程");
        assertThat((List<?>) result.get("sources")).isNotEmpty();
    }

    @Test
    void viewerCannotSeeOperatorOrAdminDocuments() {
        List<RagDocument> documents = ragService.list(session("VIEWER"));

        assertThat(documents).extracting(RagDocument::getTitle)
                .doesNotContain("入库质检规范", "管理员权限规范")
                .contains("采购下单流程");
    }

    @Test
    void adminCanSeeAllVisibilityLevels() {
        List<RagDocument> documents = ragService.list(session("ADMIN"));

        assertThat(documents).extracting(RagDocument::getTitle)
                .contains("采购下单流程", "入库质检规范", "管理员权限规范");
    }

    @Test
    void adminCanUploadAndDeleteDocument() {
        RagDocument document = ragService.add("测试知识",
                "这是用于自动化测试的知识库内容。", "PUBLIC", session("ADMIN"));

        assertThat(ragService.list(session("ADMIN")))
                .extracting(RagDocument::getTitle).contains("测试知识");
        assertThat(ragService.delete(document.getId())).isTrue();
        assertThat(ragService.list(session("ADMIN")))
                .extracting(RagDocument::getTitle).doesNotContain("测试知识");
    }

    @Test
    void adminEvaluationPassesAllTenCases() {
        Map<String, Object> result = ragService.evaluate(session("ADMIN"));

        assertThat(result.get("total")).isEqualTo(10);
        assertThat(result.get("hits")).isEqualTo(10);
        assertThat((Double) result.get("hitRate")).isEqualTo(1.0);
    }

    @Test
    void blankQuestionAndDocumentAreRejected() {
        assertThatThrownBy(() -> ragService.ask(" ", session("ADMIN"), 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ragService.add(" ", "content", "PUBLIC", session("ADMIN")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
