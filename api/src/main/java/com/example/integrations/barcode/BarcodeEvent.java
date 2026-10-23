package com.example.integrations.barcode;

public class BarcodeEvent {

    private final String businessName;
    private final String operationType;
    private final String billNo;
    private final String warehouse;
    private final String businessTime;
    private final String recordStatus;

    public BarcodeEvent(String businessName, String operationType, String billNo,
                        String warehouse, String businessTime, String recordStatus) {
        this.businessName = businessName;
        this.operationType = operationType;
        this.billNo = billNo;
        this.warehouse = warehouse;
        this.businessTime = businessTime;
        this.recordStatus = recordStatus;
    }

    public String getBusinessName() { return businessName; }
    public String getOperationType() { return operationType; }
    public String getBillNo() { return billNo; }
    public String getWarehouse() { return warehouse; }
    public String getBusinessTime() { return businessTime; }
    public String getRecordStatus() { return recordStatus; }
}
