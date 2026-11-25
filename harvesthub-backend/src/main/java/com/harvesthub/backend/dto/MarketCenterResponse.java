package com.harvesthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Centre highlight card. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketCenterResponse {
    private String name;
    private String province;
    private String recommendation;   // HARVEST NOW, DELAY HARVEST, HOLD
    private String supply;           // Low / Medium / High
    private int supplyPercent;
    private String demand;
    private int demandPercent;
    private String updateTime;       // "15 mins ago"
    private boolean highlighted;
    private Double latestPrice;
}
