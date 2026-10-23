package com.example.integrations.billtrace;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billtrace")
public class BillTraceController {

    private final BillTraceService service;

    public BillTraceController(BillTraceService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> trace(@RequestParam String billType, @RequestParam String id) {
        BillTraceNode node = service.trace(billType, id);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (node == null) {
            result.put("code", 404);
            result.put("message", "单据不存在");
            result.put("data", null);
        } else {
            result.put("code", 200);
            result.put("message", "操作成功");
            result.put("data", node);
        }
        return result;
    }
}
