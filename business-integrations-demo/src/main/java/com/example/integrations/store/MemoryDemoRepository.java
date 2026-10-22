package com.example.integrations.store;

import com.example.integrations.barcode.BarcodeEvent;
import com.example.integrations.billtrace.BillTraceNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
@ConditionalOnProperty(name = "demo.storage", havingValue = "memory", matchIfMissing = true)
public class MemoryDemoRepository implements DemoRepository {

    private final Map<String, List<BarcodeEvent>> events = new LinkedHashMap<String, List<BarcodeEvent>>();
    private final Map<String, BillTraceNode> nodes = new LinkedHashMap<String, BillTraceNode>();
    private final Map<String, List<String>> next = new LinkedHashMap<String, List<String>>();
    private final Map<String, List<String>> previous = new LinkedHashMap<String, List<String>>();
    private final Map<String, String> users = new LinkedHashMap<String, String>();

    public MemoryDemoRepository() {
        events.put("BC001", Arrays.asList(
                new BarcodeEvent("标签打印", "PRINT", "LABEL-1001", "一号仓", "2026-10-01 09:00:00", "SUCCESS"),
                new BarcodeEvent("来料入库", "INBOUND", "RK-1001", "一号仓", "2026-10-01 10:00:00", "SUCCESS"),
                new BarcodeEvent("生产领料", "ISSUE", "LL-1001", "一号仓", "2026-10-02 09:00:00", "SUCCESS"),
                new BarcodeEvent("成品入库", "INBOUND", "CPRK-1001", "二号仓", "2026-10-03 15:00:00", "SUCCESS"),
                new BarcodeEvent("销售出库", "OUTBOUND", "CK-1001", "二号仓", "2026-10-04 11:00:00", "SUCCESS")
        ));
        events.put("BC002", Arrays.asList(
                new BarcodeEvent("标签打印", "PRINT", "LABEL-1002", "一号仓", "2026-10-01 09:10:00", "SUCCESS"),
                new BarcodeEvent("来料入库", "INBOUND", "RK-1002", "一号仓", "2026-10-01 10:20:00", "SUCCESS")
        ));

        addNode("SO-1001", "SALES_ORDER", "SO-1001", "销售订单");
        addNode("MO-1001", "PRODUCTION_ORDER", "MO-1001", "生产订单");
        addNode("QT-1001", "QUALITY_TEST", "QT-1001", "质检单");
        addNode("RIS-1001", "RECEIVE_IN_STOCK", "RIS-1001", "采购入库单");
        addNode("BC001", "BARCODE", "BC001", "条码");
        link("SO-1001", "MO-1001");
        link("MO-1001", "QT-1001");
        link("QT-1001", "RIS-1001");
        link("RIS-1001", "BC001");

        users.put("ys-user-001", "local-user-1001");
        users.put("13800000000", "local-user-1002");
    }

    @Override
    public Map<String, Object> traceBarcode(String barcode) {
        List<BarcodeEvent> list = events.get(barcode);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("barcode", barcode);
        result.put("found", list != null);
        result.put("quantity", 1);
        result.put("totalEvents", list == null ? 0 : list.size());
        result.put("events", list == null ? Collections.emptyList() : new ArrayList<BarcodeEvent>(list));
        return result;
    }

    @Override
    public BillTraceNode traceBill(String billType, String id) {
        BillTraceNode root = nodes.get(id);
        if (root == null) {
            return null;
        }
        BillTraceNode result = copy(root);
        result.getUpstreamNodes().addAll(traceDirection(id, previous, new HashSet<String>(Arrays.asList(id))));
        result.getDownstreamNodes().addAll(traceDirection(id, next, new HashSet<String>(Arrays.asList(id))));
        return result;
    }

    @Override
    public String findLocalUserId(String outerUserId) {
        return users.get(outerUserId);
    }

    @Override
    public void saveSfLog(String billId, String serviceCode, boolean success, String result, String sign) {
        // 内存模式不持久化；SF 服务自身仍会保留演示日志。
    }

    private List<BillTraceNode> traceDirection(String id, Map<String, List<String>> edges, Set<String> visited) {
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
            child.getUpstreamNodes().addAll(traceDirection(childId, previous, visited));
            child.getDownstreamNodes().addAll(traceDirection(childId, next, visited));
            result.add(child);
        }
        return result;
    }

    private BillTraceNode copy(BillTraceNode source) {
        return new BillTraceNode(source.getId(), source.getBillType(), source.getBillNo(),
                source.getName(), source.getStatus());
    }

    private void addNode(String id, String type, String no, String name) {
        nodes.put(id, new BillTraceNode(id, type, no, name, "已审核"));
    }

    private void link(String from, String to) {
        next.computeIfAbsent(from, key -> new ArrayList<String>()).add(to);
        previous.computeIfAbsent(to, key -> new ArrayList<String>()).add(from);
    }
}
