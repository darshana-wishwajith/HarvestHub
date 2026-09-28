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
    private ChatService chatService;

    @GetMapping("chat/{orderId}/messages")
    public ResponseEntity<List<ChatMessage>> getChatMessages(@PathVariable String orderId) {
        return ResponseEntity.ok(chatService.getMessages(orderId));
    }

    @PostMapping("chat/{orderId}/send")
    public ResponseEntity<ChatMessage> sendMessage(
            @PathVariable String orderId,
            @RequestBody SendMessageRequest request) {
        ChatMessage message = chatService.sendMessage(orderId, request.getText(), "current-user");
        return ResponseEntity.ok(message);
    }
}
