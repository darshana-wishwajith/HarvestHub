package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.SendMessageRequest;
import com.harvesthub.backend.model.ChatMessage;
import com.harvesthub.backend.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ChatController {

    @Autowired
    private com.harvesthub.backend.security.CurrentUser currentUser;

    @Autowired
    private com.harvesthub.backend.repository.OrderRepository orderRepository;

    private void requireParticipant(String orderId) {
        var order = orderRepository.findById(orderId).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Order not found"));
        currentUser.requireParticipantOrStaff(order.getFarmerId(), order.getBuyerId(), order.getTransporterId());
    }
    @Autowired
    private ChatService chatService;

    @GetMapping("chat/{orderId}/messages")
    public ResponseEntity<List<ChatMessage>> getChatMessages(@PathVariable String orderId) {
        requireParticipant(orderId);
        return ResponseEntity.ok(chatService.getMessages(orderId));
    }

    @PostMapping("chat/{orderId}/send")
    public ResponseEntity<ChatMessage> sendMessage(
            @PathVariable String orderId,
            @RequestBody SendMessageRequest request) {
        requireParticipant(orderId);
        if (request.getText() == null || request.getText().isBlank() || request.getText().length() > 2000) {
            return ResponseEntity.badRequest().build();
        }
        ChatMessage message = chatService.sendMessage(orderId, request.getText().trim(), currentUser.id());
        return ResponseEntity.ok(message);
    }
}
