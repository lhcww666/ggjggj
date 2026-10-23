package com.example.integrations.sso;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SsoTargetRegistry {

    private final Map<String, String> paths = new LinkedHashMap<String, String>();

    public SsoTargetRegistry() {
        paths.put("outsourceInStock", "/outsource-in-stock");
        paths.put("receiveInStock", "/receive-in-stock");
        paths.put("sellOutStockBill", "/sales-out-stock");
        paths.put("stockOtherOutBill", "/other-out-stock");
        paths.put("stockOtherInBill", "/other-in-stock");
        paths.put("stockAllocateBill", "/stock-allocate");
    }

    public SsoTarget resolve(String targetType, String targetId) {
        String path = paths.get(targetType);
        return path == null ? null : new SsoTarget(targetType, path, targetId);
    }
}
