package com.example.integrations.sso;

import java.util.Map;
import java.util.TreeMap;

public class SsoAuthorizeRequest {

    private String appId;
    private String userId;
    private String targetType;
    private String targetId;
    private String timestamp;
    private String nonce;
    private String sign;

    public Map<String, String> signFields() {
        Map<String, String> values = new TreeMap<String, String>();
        values.put("appId", appId);
        values.put("userId", userId);
        values.put("targetType", targetType);
        values.put("targetId", targetId);
        values.put("timestamp", timestamp);
        values.put("nonce", nonce);
        return values;
    }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }
    public String getSign() { return sign; }
    public void setSign(String sign) { this.sign = sign; }
}
