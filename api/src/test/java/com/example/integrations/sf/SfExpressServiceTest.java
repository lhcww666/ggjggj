package com.example.integrations.sf;

import com.example.integrations.BusinessTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SfExpressServiceTest extends BusinessTestSupport {

    private SfExpressService service;

    @BeforeEach
    void setUp() {
        service = new SfExpressService(properties(), new ObjectMapper(), repository, logService);
    }

    @Test
    void validOrderCreatesWaybill() {
        SfOrderResponse response = service.createOrder(order("DISPATCH-1"));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getWaybillNo()).startsWith("SF");
        assertThat(response.isIdempotent()).isFalse();
    }

    @Test
    void duplicateOrderIsIdempotent() {
        SfOrderResponse first = service.createOrder(order("DISPATCH-2"));
        SfOrderResponse second = service.createOrder(order("DISPATCH-2"));

        assertThat(second.isSuccess()).isTrue();
        assertThat(second.isIdempotent()).isTrue();
        assertThat(second.getWaybillNo()).isEqualTo(first.getWaybillNo());
    }

    @Test
    void invalidOrderIsRejected() {
        SfOrderRequest request = order("DISPATCH-3");
        request.setReceiverPhone("");

        SfOrderResponse response = service.createOrder(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void businessFailureDoesNotCreateWaybill() {
        SfOrderRequest request = order("DISPATCH-4");
        request.setFailBusiness(true);

        SfOrderResponse response = service.createOrder(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("BUSINESS_REJECTED");
        assertThat(service.logs()).isNotEmpty();
    }

    @Test
    void routeAndCancelWorkForCreatedOrder() {
        SfOrderResponse order = service.createOrder(order("DISPATCH-5"));
        Map<String, Object> route = service.route(order.getWaybillNo());
        SfOrderResponse cancelled = service.cancel("DISPATCH-5");

        assertThat(route.get("success")).isEqualTo(true);
        assertThat((Iterable<?>) route.get("routes")).hasSize(3);
        assertThat(cancelled.isSuccess()).isTrue();
    }

    @Test
    void unknownCancellationIsRejected() {
        assertThat(service.cancel("UNKNOWN").getCode()).isEqualTo("ORDER_NOT_FOUND");
    }

    private SfOrderRequest order(String dispatchBillId) {
        SfOrderRequest request = new SfOrderRequest();
        request.setDispatchBillId(dispatchBillId);
        request.setReceiverName("测试收货人");
        request.setReceiverPhone("13800000000");
        request.setReceiverAddress("上海市测试路 1 号");
        request.setCargoName("演示货物");
        request.setQuantity(2);
        return request;
    }
}
