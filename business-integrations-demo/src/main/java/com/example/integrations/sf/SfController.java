package com.example.integrations.sf;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sf")
public class SfController {

    private final SfExpressService service;

    public SfController(SfExpressService service) {
        this.service = service;
    }

    @PostMapping("/orders")
    public SfOrderResponse create(@RequestBody SfOrderRequest request) {
        return service.createOrder(request);
    }

    @PostMapping("/cancel/{dispatchBillId}")
    public SfOrderResponse cancel(@PathVariable String dispatchBillId) {
        return service.cancel(dispatchBillId);
    }

    @GetMapping("/route/{waybillNo}")
    public Map<String, Object> route(@PathVariable String waybillNo) {
        return service.route(waybillNo);
    }

    @GetMapping("/logs")
    public List<Map<String, Object>> logs() {
        return service.logs();
    }
}
