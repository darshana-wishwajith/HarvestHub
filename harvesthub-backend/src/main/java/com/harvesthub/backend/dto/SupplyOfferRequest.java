package com.harvesthub.backend.dto;
import lombok.Data;

@Data
public class SupplyOfferRequest {
    private String requestId;
    private String batchId;
    private double quantity;
    private double pricePerKg;
}
