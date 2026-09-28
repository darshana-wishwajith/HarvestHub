package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.ConfirmDealRequest;
import com.harvesthub.backend.dto.GenericResponse;
import com.harvesthub.backend.dto.TransporterEventRequest;
import com.harvesthub.backend.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping("orders/{orderId}/confirm-deal")
    public ResponseEntity<GenericResponse> confirmDeal(
            @PathVariable String orderId,
            @RequestBody ConfirmDealRequest request) {
        return ResponseEntity.ok(orderService.confirmDeal(orderId, request));
    }

    @PostMapping("/orders/create")
    public ResponseEntity<com.harvesthub.backend.model.Order> createOrder(@RequestBody com.harvesthub.backend.model.Order order) {
        return ResponseEntity.ok(orderService.createOrder(order));
    }

    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<com.harvesthub.backend.model.Order> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody java.util.Map<String, String> request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request.get("status"), request.get("note")));
    }

    @GetMapping("/orders/farmer/{farmerId}")
    public ResponseEntity<java.util.List<com.harvesthub.backend.model.Order>> getOrdersByFarmer(@PathVariable String farmerId) {
        return ResponseEntity.ok(orderService.getOrdersByFarmer(farmerId));
    }

    @GetMapping("/orders/buyer/{buyerId}")
    public ResponseEntity<java.util.List<com.harvesthub.backend.model.Order>> getOrdersByBuyer(@PathVariable String buyerId) {
        return ResponseEntity.ok(orderService.getOrdersByBuyer(buyerId));
    }

    @GetMapping("/orders/transporter/{transporterId}")
    public ResponseEntity<java.util.List<com.harvesthub.backend.model.Order>> getOrdersByTransporter(
            @PathVariable String transporterId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.getOrdersByTransporter(transporterId, status));
    }

    @PostMapping("/orders/{orderId}/transporter-events")
    public ResponseEntity<?> applyTransporterEvent(
            @PathVariable String orderId,
            @RequestBody TransporterEventRequest request) {
        try {
            com.harvesthub.backend.model.Order order = orderService.applyTransporterEvent(orderId, request);
            return order == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(order);
        } catch (IllegalStateException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @PostMapping("/orders/{orderId}/payment")
    public ResponseEntity<?> recordAdvancePayment(
            @PathVariable String orderId,
            @RequestBody java.util.Map<String, Object> request) {
        Object amountValue = request.get("amount");
        if (!(amountValue instanceof Number)) {
            return ResponseEntity.badRequest().body("Payment amount is required");
        }
        com.harvesthub.backend.model.Order order = orderService.recordAdvancePayment(
                orderId, ((Number) amountValue).doubleValue());
        return order == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(order);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<com.harvesthub.backend.model.Order> getOrder(@PathVariable String orderId) {
        com.harvesthub.backend.model.Order order = orderService.getOrderById(orderId);
        if (order == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(order);
    }

    @PostMapping("/orders/{orderId}/rate")
    public ResponseEntity<com.harvesthub.backend.model.Order> rateOrder(
            @PathVariable String orderId,
            @RequestBody java.util.Map<String, Object> request) {
        Integer rating = (Integer) request.get("rating");
        String comment = (String) request.get("comment");
        return ResponseEntity.ok(orderService.rateOrder(orderId, rating, comment));
    }
}
