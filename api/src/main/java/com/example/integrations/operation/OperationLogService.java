package com.example.integrations.operation;

import com.example.integrations.repository.DemoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OperationLogService {

    private final DemoRepository repository;

    public OperationLogService(DemoRepository repository) {
        this.repository = repository;
    }

    public void record(String userId, String action, String detail, boolean success) {
        repository.saveOperationLog(userId, action, detail, success);
    }

    public List<Map<String, Object>> list(int limit) {
        return repository.operationLogs(limit <= 0 ? 50 : Math.min(limit, 200));
    }
}
