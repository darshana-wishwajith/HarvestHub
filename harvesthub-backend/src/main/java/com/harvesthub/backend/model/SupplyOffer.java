package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "supply_offers")
public class SupplyOffer {
    @Id
    private String id;
    private String requestId;
    private String batchId;
    private String farmerId;
    private String farmerName;
    private String cropName;
    private String location;
    private String grade;
    private double quantity;
    private double pricePerKg;
    private String status;
    private String createdDate;
}
