package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "crop_batches")
public class CropBatch {
    @Id
    private String id;
    private String batchNumber;
    private String farmerId;
    private String cropType;
    private double availableQuantity;
    private boolean isVerified;
    private String createdDate;
}
