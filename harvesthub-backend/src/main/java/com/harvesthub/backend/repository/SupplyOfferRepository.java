package com.harvesthub.backend.repository;
import com.harvesthub.backend.model.SupplyOffer;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface SupplyOfferRepository extends MongoRepository<SupplyOffer, String> {
    List<SupplyOffer> findByFarmerId(String farmerId);
    List<SupplyOffer> findByRequestId(String requestId);
    List<SupplyOffer> findByRequestIdIn(List<String> requestIds);
}
