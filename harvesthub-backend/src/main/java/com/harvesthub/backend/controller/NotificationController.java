package com.harvesthub.backend.controller;

import com.harvesthub.backend.model.NotificationItem;
import com.harvesthub.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @GetMapping("notifications/{farmerId}")
    public ResponseEntity<List<NotificationItem>> getNotifications(@PathVariable String farmerId) {
        return ResponseEntity.ok(notificationService.getNotifications(farmerId));
    }

    @PostMapping("notifications/{id}/read")
    public ResponseEntity<?> markNotificationRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification marked as read");
        return ResponseEntity.ok(response);
    }

    @PostMapping("notifications/mark-all-read")
    public ResponseEntity<?> markAllNotificationsRead(@RequestParam("farmerId") String farmerId) {
        notificationService.markAllAsRead(farmerId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "All notifications marked as read");
        return ResponseEntity.ok(response);
    }
}
