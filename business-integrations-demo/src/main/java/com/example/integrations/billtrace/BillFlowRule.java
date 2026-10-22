package com.example.integrations.billtrace;

public enum BillFlowRule {
    SALES_ORDER("SALES_ORDER", "销售订单"),
    PRODUCTION_ORDER("PRODUCTION_ORDER", "生产订单"),
    QUALITY_TEST("QUALITY_TEST", "质检单"),
    RECEIVE_IN_STOCK("RECEIVE_IN_STOCK", "采购入库单"),
    BARCODE("BARCODE", "条码");

    private final String code;
    private final String label;

    BillFlowRule(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    public static BillFlowRule from(String code) {
        for (BillFlowRule rule : values()) {
            if (rule.code.equalsIgnoreCase(code)) {
                return rule;
            }
        }
        return null;
    }
}
