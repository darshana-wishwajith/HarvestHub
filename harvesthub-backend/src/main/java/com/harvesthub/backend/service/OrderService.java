package com.harvesthub.backend.service;

import com.harvesthub.backend.dto.ConfirmDealRequest;
import com.harvesthub.backend.dto.GenericResponse;
import com.harvesthub.backend.dto.TransporterEventRequest;
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

    public java.util.List<Order> getOrdersByBuyer(String buyerId) {
        return orderRepository.findByBuyerId(buyerId);
    }

    public java.util.List<Order> getOrdersByTransporter(String transporterId, String status) {
        if (status == null || status.isBlank()) {
            return orderRepository.findByTransporterId(transporterId);
        }
        return orderRepository.findByTransporterIdAndStatusIn(transporterId, java.util.List.of(status));
    }

    public Order applyTransporterEvent(String orderId, TransporterEventRequest request) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || request.getTransporterId() == null
                || !request.getTransporterId().equals(order.getTransporterId())) {
            return null;
        }

        String nextStatus = nextTransportStatus(order.getStatus(), request.getEventType());
        if (nextStatus == null) {
            throw new IllegalStateException("Invalid transporter transition from "
                    + order.getStatus() + " using " + request.getEventType());
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        if (order.getTransportEvents() == null) {
            order.setTransportEvents(new java.util.ArrayList<>());
        }
        order.getTransportEvents().add(new Order.TransportEvent(
                request.getEventType(), timestamp, request.getTransporterId(), request.getNote(),
                request.getLatitude(), request.getLongitude(), request.getPhotoCount(),
                request.getBuyerVerified(), request.getRejectionReason()));
        order.setStatus(nextStatus);
        if ("ARRIVED_AT_DESTINATION".equals(nextStatus) || "DELIVERED".equals(nextStatus)) {
            order.setActualDeliveryDate(timestamp);
        }
        if (order.getStatusHistory() == null) {
            order.setStatusHistory(new java.util.ArrayList<>());
        }
        order.getStatusHistory().add(new Order.StatusChange(
                nextStatus, timestamp, request.getNote() == null ? request.getEventType() : request.getNote()));
        return orderRepository.save(order);
    }

    private String nextTransportStatus(String currentStatus, String eventType) {
        if (eventType == null) return null;
        return switch (eventType) {
                case "ACCEPT" -> "DISPATCHING".equals(currentStatus) || "HARVEST_LOCKED".equals(currentStatus)
                    || "TRANSPORTER_ASSIGNED".equals(currentStatus)
                    ? "TRANSPORTER_ACCEPTED" : null;
            case "ARRIVED_PICKUP" -> "TRANSPORTER_ACCEPTED".equals(currentStatus) ? "ARRIVED_AT_PICKUP" : null;
            case "LOADING_EVIDENCE_SUBMITTED" -> "ARRIVED_AT_PICKUP".equals(currentStatus)
                    ? "AWAITING_BUYER_LOADING_APPROVAL" : null;
            case "START_JOURNEY" -> "AWAITING_BUYER_LOADING_APPROVAL".equals(currentStatus)
                    || "LOADING_APPROVED".equals(currentStatus) ? "IN_TRANSIT" : null;
            case "ARRIVED_DESTINATION" -> "IN_TRANSIT".equals(currentStatus) ? "ARRIVED_AT_DESTINATION" : null;
            case "DELIVERY_COMPLETED" -> "ARRIVED_AT_DESTINATION".equals(currentStatus) ? "DELIVERED" : null;
            case "DELIVERY_REJECTED" -> "ARRIVED_AT_DESTINATION".equals(currentStatus)
                    || "IN_TRANSIT".equals(currentStatus) ? "DISPUTED" : null;
            default -> null;
        };
    }

    public Order recordAdvancePayment(String orderId, double amount) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return null;
        }
        order.setEscrowStatus("ADVANCE_PAID");
        order.setTotalAmount(amount);
        order.setStatus("DISPATCHING");
        if (order.getStatusHistory() == null) {
            order.setStatusHistory(new java.util.ArrayList<>());
        }
        order.getStatusHistory().add(new Order.StatusChange(
                order.getStatus(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "Buyer advance payment recorded in escrow"));
        return orderRepository.save(order);
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
