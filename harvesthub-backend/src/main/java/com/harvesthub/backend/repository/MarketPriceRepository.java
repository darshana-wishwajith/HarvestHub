package com.harvesthub.backend.repository;

import com.harvesthub.backend.model.MarketPrice;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MarketPriceRepository extends MongoRepository<MarketPrice, String> {
    List<MarketPrice> findByCropNameIgnoreCaseAndDateGreaterThanEqualOrderByDateAsc(String cropName, String fromDate);
    List<MarketPrice> findByDateGreaterThanEqual(String fromDate);
    boolean existsByCropNameAndCenterAndDate(String cropName, String center, String date);
}
