package com.example.integrations.sf;

public class SfOrderResponse {

    private boolean success;
    private String code;
    private String message;
    private String dispatchBillId;
    private String waybillNo;
    private boolean idempotent;

    public SfOrderResponse(boolean success, String code, String message,
                           String dispatchBillId, String waybillNo, boolean idempotent) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.dispatchBillId = dispatchBillId;
        this.waybillNo = waybillNo;
        this.idempotent = idempotent;
    }

    public boolean isSuccess() { return success; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public String getDispatchBillId() { return dispatchBillId; }
    public String getWaybillNo() { return waybillNo; }
    public boolean isIdempotent() { return idempotent; }
}
