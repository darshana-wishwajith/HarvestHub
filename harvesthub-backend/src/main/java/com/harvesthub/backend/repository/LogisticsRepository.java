package com.harvesthub.backend.repository;

import com.harvesthub.backend.model.LogisticsRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface LogisticsRepository extends MongoRepository<LogisticsRecord, String> {
    Optional<LogisticsRecord> findByOrderId(String orderId);
}
