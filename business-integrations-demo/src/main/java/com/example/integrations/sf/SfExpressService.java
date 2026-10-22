package com.example.integrations.sf;

import com.example.integrations.config.IntegrationProperties;
import com.example.integrations.store.DemoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class SfExpressService {

    private final IntegrationProperties properties;
    private final ObjectMapper mapper;
    private final DemoRepository repository;
    private final ConcurrentMap<String, SfOrderResponse> orders = new ConcurrentHashMap<String, SfOrderResponse>();
    private final ConcurrentMap<String, Boolean> cancelled = new ConcurrentHashMap<String, Boolean>();
    private final List<Map<String, Object>> pushLogs = new ArrayList<Map<String, Object>>();

    public SfExpressService(IntegrationProperties properties, ObjectMapper mapper, DemoRepository repository) {
        this.properties = properties;
        this.mapper = mapper;
        this.repository = repository;
    }

    public SfOrderResponse createOrder(SfOrderRequest request) {
        if (isBlank(request.getDispatchBillId()) || isBlank(request.getReceiverName())
                || isBlank(request.getReceiverPhone()) || isBlank(request.getReceiverAddress())
                || isBlank(request.getCargoName()) || request.getQuantity() == null || request.getQuantity() <= 0) {
            return new SfOrderResponse(false, "INVALID_REQUEST", "发货单、收件人和货品信息必须完整",
                    request.getDispatchBillId(), null, false);
        }
        SfOrderResponse existing = orders.get(request.getDispatchBillId());
        if (existing != null) {
            return new SfOrderResponse(true, "ORDER_EXISTS", "该发货单已完成顺丰下单",
                    existing.getDispatchBillId(), existing.getWaybillNo(), true);
        }

        Map<String, Object> msgData = buildMessage(request);
        try {
            String msgJson = mapper.writeValueAsString(msgData);
            String timestamp = String.valueOf(System.currentTimeMillis());
            String sign = SfSignUtil.sign(msgJson, timestamp, properties.getSfCheckWord());

            // 第一层：网关层；第二层：业务层。这里用开关模拟业务失败。
            boolean gatewaySuccess = true;
            boolean businessSuccess = !request.isFailBusiness();
            if (!gatewaySuccess || !businessSuccess) {
                log(request.getDispatchBillId(), "CREATE", false,
                        businessSuccess ? "GATEWAY_REJECTED" : "BUSINESS_REJECTED", sign);
                return new SfOrderResponse(false, businessSuccess ? "GATEWAY_REJECTED" : "BUSINESS_REJECTED",
                        "顺丰业务校验失败", request.getDispatchBillId(), null, false);
            }

            String waybillNo = "SF" + System.currentTimeMillis();
            SfOrderResponse response = new SfOrderResponse(true, "SUCCESS", "顺丰下单成功",
                    request.getDispatchBillId(), waybillNo, false);
            orders.put(request.getDispatchBillId(), response);
            log(request.getDispatchBillId(), "CREATE", true, "SUCCESS", sign);
            return response;
        } catch (Exception exception) {
            log(request.getDispatchBillId(), "CREATE", false, "SIGN_OR_REQUEST_FAILED", "");
            return new SfOrderResponse(false, "REQUEST_FAILED", "顺丰请求失败",
                    request.getDispatchBillId(), null, false);
        }
    }

    public SfOrderResponse cancel(String dispatchBillId) {
        SfOrderResponse order = orders.get(dispatchBillId);
        if (order == null) {
            return new SfOrderResponse(false, "ORDER_NOT_FOUND", "未找到顺丰订单",
                    dispatchBillId, null, false);
        }
        cancelled.put(dispatchBillId, Boolean.TRUE);
        log(dispatchBillId, "CANCEL", true, "SUCCESS", "");
        return new SfOrderResponse(true, "SUCCESS", "顺丰订单取消成功",
                dispatchBillId, order.getWaybillNo(), false);
    }

    public Map<String, Object> route(String waybillNo) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("waybillNo", waybillNo);
        if (waybillNo == null || !waybillNo.startsWith("SF")) {
            result.put("success", false);
            result.put("message", "运单号不存在");
            return result;
        }
        result.put("success", true);
        result.put("routes", Arrays.asList(
                routeEvent("2026-10-20 09:00", "已揽收", "深圳"),
                routeEvent("2026-10-20 18:00", "运输中", "广州"),
                routeEvent("2026-10-21 10:00", "派送中", "上海")
        ));
        return result;
    }

    public List<Map<String, Object>> logs() {
        return new ArrayList<Map<String, Object>>(pushLogs);
    }

    private Map<String, Object> buildMessage(SfOrderRequest request) {
        Map<String, Object> msg = new LinkedHashMap<String, Object>();
        msg.put("orderId", request.getDispatchBillId());
        msg.put("customerCode", properties.getSfCustomerCode());
        msg.put("contactInfoList", Arrays.asList(
                contact("寄件人", "寄件人地址", "13800000001"),
                contact(request.getReceiverName(), request.getReceiverAddress(), request.getReceiverPhone())
        ));
        Map<String, Object> cargo = new LinkedHashMap<String, Object>();
        cargo.put("name", request.getCargoName());
        cargo.put("count", request.getQuantity());
        msg.put("cargoDetails", Arrays.asList(cargo));
        return msg;
    }

    private Map<String, Object> contact(String name, String address, String phone) {
        Map<String, Object> contact = new LinkedHashMap<String, Object>();
        contact.put("contact", name);
        contact.put("address", address);
        contact.put("mobile", phone);
        return contact;
    }

    private Map<String, Object> routeEvent(String time, String status, String address) {
        Map<String, Object> event = new LinkedHashMap<String, Object>();
        event.put("time", time);
        event.put("status", status);
        event.put("address", address);
        return event;
    }

    private void log(String billId, String service, boolean success, String result, String sign) {
        Map<String, Object> log = new LinkedHashMap<String, Object>();
        log.put("billId", billId);
        log.put("service", service);
        log.put("success", success);
        log.put("result", result);
        log.put("sign", sign);
        log.put("time", System.currentTimeMillis());
        pushLogs.add(log);
        repository.saveSfLog(billId, service, success, result, sign);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
