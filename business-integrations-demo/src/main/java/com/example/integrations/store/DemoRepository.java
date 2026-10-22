package com.example.integrations.store;

import com.example.integrations.billtrace.BillTraceNode;

import java.util.Map;

public interface DemoRepository {

    Map<String, Object> traceBarcode(String barcode);

    BillTraceNode traceBill(String billType, String id);

    String findLocalUserId(String outerUserId);

    void saveSfLog(String billId, String serviceCode, boolean success, String result, String sign);
}
