package com.harvesthub.backend.repository;
import com.harvesthub.backend.model.CropBatch;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface CropBatchRepository extends MongoRepository<CropBatch, String> {
    List<CropBatch> findByFarmerIdAndCropTypeAndIsVerifiedTrue(String farmerId, String cropType);
    List<CropBatch> findByFarmerId(String farmerId);
}
