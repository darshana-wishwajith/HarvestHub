package com.harvesthub.backend.repository;
import com.harvesthub.backend.model.WantedAd;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface WantedAdRepository extends MongoRepository<WantedAd, String> {
    List<WantedAd> findByCropNameContainingIgnoreCase(String cropName);
    List<WantedAd> findByBuyerId(String buyerId);
}
