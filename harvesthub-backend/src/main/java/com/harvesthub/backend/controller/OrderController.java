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
}
