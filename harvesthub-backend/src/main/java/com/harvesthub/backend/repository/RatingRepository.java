package com.harvesthub.backend.repository;

import com.harvesthub.backend.model.Rating;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface RatingRepository extends MongoRepository<Rating, String> {
    List<Rating> findByToUserId(String toUserId);
    List<Rating> findByOrderId(String orderId);
    boolean existsByOrderIdAndFromUserIdAndToUserId(String orderId, String fromUserId, String toUserId);
}
