package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.ConfirmDealRequest;
import com.harvesthub.backend.dto.GenericResponse;
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
