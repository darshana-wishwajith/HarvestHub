package com.harvesthub.backend.repository;
import com.harvesthub.backend.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findByFarmerIdOrderByTimestampDesc(String farmerId, Pageable pageable);
    List<Transaction> findByFarmerId(String farmerId);
}
