package com.example.integrations.billtrace;

import com.example.integrations.repository.DemoRepository;
import org.springframework.stereotype.Service;

@Service
public class BillTraceService {

    private final DemoRepository repository;

    public BillTraceService(DemoRepository repository) {
        this.repository = repository;
    }

    public BillTraceNode trace(String billType, String id) {
        return repository.traceBill(billType, id);
    }
}
