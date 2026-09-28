package com.harvesthub.backend.service;

import com.harvesthub.backend.dto.ConfirmDealRequest;
import com.harvesthub.backend.dto.GenericResponse;
import com.harvesthub.backend.model.Order;
import com.harvesthub.backend.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    public GenericResponse confirmDeal(String orderId, ConfirmDealRequest request) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return new GenericResponse(false, "Order not found");
        }
        order.setQuantity(request.getQuantity());
        order.setPricePerKg(request.getPricePerKg());
        order.setDeliveryDate(request.getDeliveryDate());
        order.setStatus("CONFIRMED");
        orderRepository.save(order);
        return new GenericResponse(true, "Deal confirmed successfully");
    }
}
