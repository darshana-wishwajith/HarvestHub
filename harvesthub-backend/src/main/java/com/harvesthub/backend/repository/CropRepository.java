package com.harvesthub.backend.repository;

import com.harvesthub.backend.model.Crop;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface CropRepository extends MongoRepository<Crop, String> {
    Optional<Crop> findFirstByNameIgnoreCase(String name);
}
