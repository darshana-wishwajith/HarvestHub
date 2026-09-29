package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "wanted_ads")
public class WantedAd {
    @Id
    private String requestId;
    private String cropName;
    private String location;
    private double requiredQuantityKg;
    private double targetPricePerKg;
    private boolean isUrgent;
    private String buyerId;
    private String createdDate;
    private String deadline;
    private String targetGrade;
    private String status;
}

