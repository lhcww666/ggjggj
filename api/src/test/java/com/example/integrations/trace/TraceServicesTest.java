package com.example.integrations.trace;

import com.example.integrations.BusinessTestSupport;
import com.example.integrations.barcode.BarcodeTraceService;
import com.example.integrations.billtrace.BillTraceNode;
import com.example.integrations.billtrace.BillTraceService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TraceServicesTest extends BusinessTestSupport {

    private final BarcodeTraceService barcodeService = new BarcodeTraceService(repository);
    private final BillTraceService billTraceService = new BillTraceService(repository);

    @Test
    void barcodeReturnsCompleteEventTimeline() {
        Map<String, Object> result = barcodeService.trace("BC001");

        assertThat(result.get("found")).isEqualTo(true);
        assertThat(result.get("totalEvents")).isEqualTo(5);
    }

    @Test
    void unknownBarcodeReturnsEmptyResult() {
        Map<String, Object> result = barcodeService.trace("UNKNOWN");

        assertThat(result.get("found")).isEqualTo(false);
        assertThat(result.get("totalEvents")).isEqualTo(0);
    }

    @Test
    void billTraceContainsDownstreamDocuments() {
        BillTraceNode result = billTraceService.trace("SALES_ORDER", "SO-1001");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("销售订单");
        assertThat(result.getDownstreamNodes()).isNotEmpty();
        assertThat(result.getDownstreamNodes().get(0).getName()).isEqualTo("生产订单");
    }

    @Test
    void unknownBillReturnsNull() {
        assertThat(billTraceService.trace("SALES_ORDER", "NOT-FOUND")).isNull();
    }
}
