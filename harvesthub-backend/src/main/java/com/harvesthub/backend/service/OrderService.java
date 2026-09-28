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

    public Order createOrder(Order order) {
        order.setCreatedDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        if (order.getStatus() == null) {
            order.setStatus("PENDING_BUYER_APPROVAL");
        }
        order.setStatusHistory(new java.util.ArrayList<>());
        order.getStatusHistory().add(new Order.StatusChange(order.getStatus(), order.getCreatedDate(), "Order created"));
        return orderRepository.save(order);
    }

    public Order updateOrderStatus(String orderId, String newStatus, String note) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setStatus(newStatus);
            if (order.getStatusHistory() == null) {
                order.setStatusHistory(new java.util.ArrayList<>());
            }
            order.getStatusHistory().add(new Order.StatusChange(newStatus, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), note));
            return orderRepository.save(order);
        }
        return null;
    }

    public java.util.List<Order> getOrdersByFarmer(String farmerId) {
        return orderRepository.findByFarmerId(farmerId);
    }

    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    public Order rateOrder(String orderId, Integer rating, String comment) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setRating(rating);
            order.setRatingComment(comment);
            return orderRepository.save(order);
        }
        return null;
    }
}
