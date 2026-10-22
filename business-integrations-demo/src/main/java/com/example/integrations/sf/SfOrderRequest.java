package com.example.integrations.sf;

public class SfOrderRequest {

    private String dispatchBillId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String cargoName;
    private Integer quantity;
    private boolean failBusiness;

    public String getDispatchBillId() { return dispatchBillId; }
    public void setDispatchBillId(String dispatchBillId) { this.dispatchBillId = dispatchBillId; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getReceiverAddress() { return receiverAddress; }
    public void setReceiverAddress(String receiverAddress) { this.receiverAddress = receiverAddress; }
    public String getCargoName() { return cargoName; }
    public void setCargoName(String cargoName) { this.cargoName = cargoName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public boolean isFailBusiness() { return failBusiness; }
    public void setFailBusiness(boolean failBusiness) { this.failBusiness = failBusiness; }
}
