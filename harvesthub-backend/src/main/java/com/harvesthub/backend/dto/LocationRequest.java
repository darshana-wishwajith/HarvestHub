package com.harvesthub.backend.dto;
import lombok.Data;

@Data
public class LocationRequest {
    private String farmerId;
    private double latitude;
    private double longitude;
}
