package com.example.integrations.barcode;

import com.example.integrations.store.DemoRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class BarcodeTraceService {

    private final DemoRepository repository;

    public BarcodeTraceService(DemoRepository repository) {
        this.repository = repository;
    }

    public Map<String, Object> trace(String barcode) {
        return repository.traceBarcode(barcode);
    }

    public int backfill() {
        return 0;
    }
}
