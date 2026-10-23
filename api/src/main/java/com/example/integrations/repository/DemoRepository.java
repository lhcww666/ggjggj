package com.example.integrations.repository;

import com.example.integrations.billtrace.BillTraceNode;

import java.util.Map;
import java.util.List;

public interface DemoRepository {

    Map<String, Object> traceBarcode(String barcode);

    BillTraceNode traceBill(String billType, String id);

    String findLocalUserId(String outerUserId);

    void saveSfLog(String billId, String serviceCode, boolean success, String result, String sign);

    void saveOperationLog(String userId, String action, String detail, boolean success);

    List<Map<String, Object>> operationLogs(int limit);
}
