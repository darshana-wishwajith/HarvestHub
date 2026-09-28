package com.harvesthub.backend.service;

import com.harvesthub.backend.model.NotificationItem;
import com.harvesthub.backend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;

    public List<NotificationItem> getNotifications(String farmerId) {
        return notificationRepository.findByFarmerIdOrderByTimestampDesc(farmerId);
    }

    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    public void markAllAsRead(String farmerId) {
        List<NotificationItem> unread = notificationRepository.findByFarmerIdAndIsReadFalse(farmerId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    public NotificationItem createNotification(String farmerId, String type, String title, String body) {
        NotificationItem notification = new NotificationItem();
        notification.setFarmerId(farmerId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setTimestamp(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        notification.setRead(false);
        return notificationRepository.save(notification);
    }
}
