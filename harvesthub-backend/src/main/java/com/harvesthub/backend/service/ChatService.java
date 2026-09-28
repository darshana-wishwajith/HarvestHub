package com.harvesthub.backend.service;

import com.harvesthub.backend.model.ChatMessage;
import com.harvesthub.backend.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ChatService {
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    public List<ChatMessage> getMessages(String orderId) {
        return chatMessageRepository.findByOrderIdOrderByTimestampAsc(orderId);
    }

    public ChatMessage sendMessage(String orderId, String text, String senderId) {
        ChatMessage message = new ChatMessage();
        message.setOrderId(orderId);
        message.setSenderId(senderId);
        message.setText(text);
        message.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        message.setSentByMe(true);
        message.setRead(false);
        return chatMessageRepository.save(message);
    }
}
