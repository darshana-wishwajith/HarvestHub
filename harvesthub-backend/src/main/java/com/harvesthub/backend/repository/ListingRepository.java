package com.harvesthub.backend.repository;
import com.harvesthub.backend.model.Listing;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ListingRepository extends MongoRepository<Listing, String> {
    List<Listing> findByFarmerIdAndStatusIn(String farmerId, List<String> statuses);
    List<Listing> findByFarmerId(String farmerId);
    List<Listing> findByFarmerIdAndCropNameContainingIgnoreCase(String farmerId, String cropName);
}
