package com.example.integrations.billtrace;

import java.util.ArrayList;
import java.util.List;

public class BillTraceNode {

    private String id;
    private String billType;
    private String billNo;
    private String name;
    private String status;
    private List<BillTraceNode> upstreamNodes = new ArrayList<BillTraceNode>();
    private List<BillTraceNode> downstreamNodes = new ArrayList<BillTraceNode>();

    public BillTraceNode() {
    }

    public BillTraceNode(String id, String billType, String billNo, String name, String status) {
        this.id = id;
        this.billType = billType;
        this.billNo = billNo;
        this.name = name;
        this.status = status;
    }

    public String getId() { return id; }
    public String getBillType() { return billType; }
    public String getBillNo() { return billNo; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public List<BillTraceNode> getUpstreamNodes() { return upstreamNodes; }
    public List<BillTraceNode> getDownstreamNodes() { return downstreamNodes; }
}
