package com.harvesthub.mobile.model;

/** One economic centre on Market Insights (backend MarketCenterResponse). */
public class MarketCenterInfo {
    private String name;
    private String province;
    private String recommendation;   // HARVEST NOW, DELAY HARVEST, HOLD
    private String supply;
    private int supplyPercent;
    private String demand;
    private int demandPercent;
    private String updateTime;
    private boolean highlighted;
    private Double latestPrice;

    public String getName() { return name; }
    public String getProvince() { return province; }
    public String getRecommendation() { return recommendation; }
    public String getSupply() { return supply; }
    public int getSupplyPercent() { return supplyPercent; }
    public String getDemand() { return demand; }
    public int getDemandPercent() { return demandPercent; }
    public String getUpdateTime() { return updateTime; }
    public boolean isHighlighted() { return highlighted; }
    public Double getLatestPrice() { return latestPrice; }
}
