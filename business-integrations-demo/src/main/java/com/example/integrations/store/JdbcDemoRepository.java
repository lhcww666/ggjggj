package com.example.integrations.store;

import com.example.integrations.billtrace.BillTraceNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
@ConditionalOnProperty(name = "demo.storage", havingValue = "jdbc")
public class JdbcDemoRepository implements DemoRepository {

    private final JdbcTemplate jdbc;

    public JdbcDemoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<String, Object> traceBarcode(String barcode) {
        List<Map<String, Object>> events = jdbc.query(
                "SELECT business_name, operation_type, bill_no, warehouse, business_time, record_status " +
                        "FROM barcode_flow_record WHERE barcode = ? ORDER BY business_time",
                (rs, rowNum) -> {
                    Timestamp time = rs.getTimestamp("business_time");
                    Map<String, Object> event = new LinkedHashMap<String, Object>();
                    event.put("businessName", rs.getString("business_name"));
                    event.put("operationType", rs.getString("operation_type"));
                    event.put("billNo", rs.getString("bill_no"));
                    event.put("warehouse", rs.getString("warehouse"));
                    event.put("businessTime", time == null ? null : time.toLocalDateTime().toString());
                    event.put("recordStatus", rs.getString("record_status"));
                    return event;
                }, barcode);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("barcode", barcode);
        result.put("found", !events.isEmpty());
        result.put("quantity", 1);
        result.put("totalEvents", events.size());
        result.put("events", events);
        return result;
    }

    @Override
    public BillTraceNode traceBill(String billType, String id) {
        Map<String, BillTraceNode> nodes = new HashMap<String, BillTraceNode>();
        jdbc.query("SELECT id, bill_type, bill_no, name, status FROM bill_trace_node",
                rs -> {
                    nodes.put(rs.getString("id"), new BillTraceNode(
                            rs.getString("id"), rs.getString("bill_type"),
                            rs.getString("bill_no"), rs.getString("name"), rs.getString("status")));
                });
        Map<String, List<String>> next = new HashMap<String, List<String>>();
        Map<String, List<String>> previous = new HashMap<String, List<String>>();
        jdbc.query("SELECT from_id, to_id FROM bill_trace_edge", rs -> {
            String from = rs.getString("from_id");
            String to = rs.getString("to_id");
            next.computeIfAbsent(from, key -> new ArrayList<String>()).add(to);
            previous.computeIfAbsent(to, key -> new ArrayList<String>()).add(from);
        });
        BillTraceNode root = nodes.get(id);
        if (root == null) {
            return null;
        }
        BillTraceNode result = copy(root);
        Set<String> visited = new HashSet<String>();
        visited.add(id);
        result.getUpstreamNodes().addAll(traceDirection(id, previous, nodes, visited));
        result.getDownstreamNodes().addAll(traceDirection(id, next, nodes, visited));
        return result;
    }

    @Override
    public String findLocalUserId(String outerUserId) {
        List<String> rows = jdbc.query(
                "SELECT local_user_id FROM sso_user_mapping WHERE outer_user_id = ?",
                (rs, rowNum) -> rs.getString("local_user_id"), outerUserId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public void saveSfLog(String billId, String serviceCode, boolean success, String result, String sign) {
        jdbc.update("INSERT INTO sf_push_log (bill_id, service_code, success, result_message, msg_digest) " +
                        "VALUES (?, ?, ?, ?, ?)",
                billId, serviceCode, success ? 1 : 0, result, sign);
    }

    private List<BillTraceNode> traceDirection(String id, Map<String, List<String>> edges,
                                                Map<String, BillTraceNode> nodes, Set<String> visited) {
        List<BillTraceNode> result = new ArrayList<BillTraceNode>();
        List<String> children = edges.get(id);
        if (children == null) {
            return result;
        }
        for (String childId : children) {
            if (visited.contains(childId)) {
                continue;
            }
            visited.add(childId);
            BillTraceNode child = copy(nodes.get(childId));
            child.getUpstreamNodes().addAll(traceDirection(childId, edges, nodes, visited));
            child.getDownstreamNodes().addAll(traceDirection(childId, edges, nodes, visited));
            result.add(child);
        }
        return result;
    }

    private BillTraceNode copy(BillTraceNode source) {
        return new BillTraceNode(source.getId(), source.getBillType(), source.getBillNo(),
                source.getName(), source.getStatus());
    }
}
