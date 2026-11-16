package com.harvesthub.backend.repository;

import com.harvesthub.backend.model.Dispute;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DisputeRepository extends MongoRepository<Dispute, String> {
    List<Dispute> findByOrderIdOrderByCreatedAtDesc(String orderId);
    List<Dispute> findByStatus(String status);
}
