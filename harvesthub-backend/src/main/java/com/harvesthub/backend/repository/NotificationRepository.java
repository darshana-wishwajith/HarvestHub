package com.harvesthub.backend.repository;
import com.harvesthub.backend.model.NotificationItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NotificationRepository extends MongoRepository<NotificationItem, String> {
    List<NotificationItem> findByFarmerIdOrderByTimestampDesc(String farmerId);
    List<NotificationItem> findByFarmerIdAndIsReadFalse(String farmerId);
}
