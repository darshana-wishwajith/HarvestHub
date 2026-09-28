package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "listings")
public class Listing {
    @Id
    private String id;
    private String farmerId;
    private String cropName;
    private String location;
    private String quantity;
    private String price;
    private String createdDate;
    private String updatedTime;
    private String status;
    private String buyerName;
    private String quantityKg;
    private String totalValue;
    private String finalPricePerKg;
}
