package com.example.integrations.barcode;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/barcodeFlowRecord")
public class BarcodeTraceController {

    private final BarcodeTraceService service;

    public BarcodeTraceController(BarcodeTraceService service) {
        this.service = service;
    }

    @GetMapping("/trace/{barcode}")
    public Map<String, Object> trace(@PathVariable String barcode) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("code", 200);
        result.put("message", "操作成功");
        result.put("data", service.trace(barcode));
        return result;
    }

    @PostMapping("/backfill")
    public ResponseEntity<Map<String, Object>> backfill(
            @RequestHeader(value = "token", required = false) String token) {
        if (token == null || token.trim().isEmpty()) {
            Map<String, Object> error = new LinkedHashMap<String, Object>();
            error.put("code", 401);
            error.put("message", "登录状态已失效");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("code", 200);
        result.put("message", "操作成功");
        result.put("data", service.backfill());
        return ResponseEntity.ok(result);
    }
}
