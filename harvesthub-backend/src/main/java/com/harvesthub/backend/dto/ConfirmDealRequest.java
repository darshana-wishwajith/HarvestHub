package com.harvesthub.backend.dto;
import lombok.Data;

@Data
public class ConfirmDealRequest {
    private double quantity;
    private double pricePerKg;
    private String deliveryDate;
}
