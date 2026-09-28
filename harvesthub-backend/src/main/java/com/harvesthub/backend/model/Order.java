package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {
    @Id
    private String orderId;
    private String farmerId;
    private String buyerId;
    private String listingId;
    private double quantity;
    private double pricePerKg;
    private String deliveryDate;
    private String status;
    private String createdDate;
}
