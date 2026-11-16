package com.harvesthub.backend.service;

import com.harvesthub.backend.model.NotificationItem;
import com.harvesthub.backend.repository.NotificationRepository;
import com.harvesthub.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * In-app notifications. {@code farmerId} on a notification is the receiving user's id (any role).
 * A user can switch a notification category off in Settings → Notification Preferences.
 */
@Service
public class NotificationService {

    /** Categories shown as toggles in Settings. */
    public static final List<String> CATEGORIES = List.of("ORDERS", "PAYMENTS", "LISTINGS", "MARKET");

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;

    public List<NotificationItem> getNotifications(String farmerId) {
        return notificationRepository.findByFarmerIdOrderByTimestampDesc(farmerId);
    }

    /** Marks a notification read only if it belongs to the given user. */
    public void markAsRead(String notificationId, String userId) {
        notificationRepository.findById(notificationId).filter(n -> userId.equals(n.getFarmerId())).ifPresent(n -> {
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

    /** Sends unless the user switched this category off. */
    public void notify(String userId, String category, String type, String title, String body) {
        if (userId == null) return;
        boolean enabled = userRepository.findById(userId)
                .map(u -> u.getNotificationPreferences())
                .map((Map<String, Boolean> prefs) -> !Boolean.FALSE.equals(prefs.get(category)))
                .orElse(true);
        if (enabled) createNotification(userId, type, title, body);
    }
}
