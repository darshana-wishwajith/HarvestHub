package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessage {
    @Id
    private String id;
    private String orderId;
    private String senderId;
    private String text;
    private String timestamp;
    @com.fasterxml.jackson.annotation.JsonProperty("isSentByMe")
    private boolean isSentByMe;
    @com.fasterxml.jackson.annotation.JsonProperty("isRead")
    private boolean isRead;
}
