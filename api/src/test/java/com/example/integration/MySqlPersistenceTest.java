package com.example.integration;

import com.example.agentdemo.context.AgentContext;
import com.example.agentdemo.context.AgentContextService;
import com.example.agentdemo.repository.AgentStore;
import com.example.agentdemo.team.AgentTeamEvaluationService;
import com.example.integrations.barcode.BarcodeTraceService;
import com.example.integrations.operation.OperationLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_IT", matches = "true")
@Transactional
class MySqlPersistenceTest {

    @Autowired
    private AgentStore agentStore;

    @Autowired
    private AgentContextService contextService;

    @Autowired
    private BarcodeTraceService barcodeTraceService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private AgentTeamEvaluationService agentTeamEvaluationService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void agentDraftAndConfirmationArePersistentAndIdempotent() {
        assertThat(agentStore.stock("A001")).isEqualTo(12);

        String draftId = agentStore.createDraft("A001", 5);
        String firstOrder = agentStore.confirm(draftId);
        String secondOrder = agentStore.confirm(draftId);

        assertThat(firstOrder).startsWith("PO-");
        assertThat(secondOrder).isEqualTo(firstOrder);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent_purchase_order WHERE draft_id = ?",
                Integer.class, draftId
        )).isEqualTo(1);
    }

    @Test
    void agentContextCanBeStoredAndResolved() {
        AgentContext context = contextService.create("mysql-user", "inventory-specialist");

        AgentContext resolved = contextService.resolve(context.getContextId());

        assertThat(resolved).isNotNull();
        assertThat(resolved.getUserId()).isEqualTo("mysql-user");
        assertThat(resolved.getAgentCode()).isEqualTo("inventory-specialist");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent_context WHERE context_id = ?",
                Integer.class, context.getContextId()
        )).isEqualTo(1);
    }

    @Test
    void businessReferenceDataIsInitializedOnce() {
        Map<String, Object> barcode = barcodeTraceService.trace("BC001");

        assertThat(barcode.get("totalEvents")).isEqualTo(5);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM barcode_flow_record",
                Integer.class
        )).isEqualTo(7);
    }

    @Test
    void operationLogsUseTheSharedDatabase() {
        operationLogService.record("mysql-user", "MYSQL_TEST", "共享数据源验证", true);

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM operation_log WHERE user_id = ? AND action = ?",
                Integer.class, "mysql-user", "MYSQL_TEST"
        )).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void routingEvaluationDoesNotPersistAgentSideEffects() {
        int contextsBefore = jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent_context WHERE user_id = ?",
                Integer.class, "agent-team-evaluator"
        );
        int draftsBefore = jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent_purchase_draft",
                Integer.class
        );

        Map<String, Object> evaluation = agentTeamEvaluationService.evaluate();

        assertThat(evaluation.get("hits")).isEqualTo(11);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent_context WHERE user_id = ?",
                Integer.class, "agent-team-evaluator"
        )).isEqualTo(contextsBefore);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent_purchase_draft",
                Integer.class
        )).isEqualTo(draftsBefore);
    }
}
