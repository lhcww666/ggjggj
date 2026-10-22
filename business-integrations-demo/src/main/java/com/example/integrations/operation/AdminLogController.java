package com.example.integrations.operation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminLogController {

    private final OperationLogService logService;

    public AdminLogController(OperationLogService logService) {
        this.logService = logService;
    }

    @GetMapping("/logs")
    public Map<String, Object> logs(@RequestParam(defaultValue = "50") int limit) {
        List<Map<String, Object>> logs = logService.list(limit);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", true);
        result.put("code", 200);
        result.put("message", "成功");
        result.put("data", logs);
        return result;
    }
}
