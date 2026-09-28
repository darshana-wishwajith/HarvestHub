package com.harvesthub.backend.repository;
import com.harvesthub.backend.model.WalletSummary;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface WalletSummaryRepository extends MongoRepository<WalletSummary, String> {
    Optional<WalletSummary> findByFarmerId(String farmerId);
}
