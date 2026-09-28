package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class NotificationItem {
    @Id
    private String id;
    private String farmerId;
    private String type;
    private String title;
    private String body;
    private String timestamp;
    private boolean isRead;
}
