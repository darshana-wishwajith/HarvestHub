package com.harvesthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Price trend for Home chart + Market Insights (Figma 10, 11). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketTrendResponse {
    private java.util.List<Double> trends;       // daily average LKR/kg, oldest first
    private String crop;
    private java.util.List<String> labels;       // yyyy-MM-dd per point
    private Double currentAverage;               // latest daily average
    private Double changePercent;                // vs first point of the window
    private String insight;                      // tip text built from the data
    private int samples;                         // price records used
}
