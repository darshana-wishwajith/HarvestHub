package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MarketTrendResponse {
    @SerializedName("trends")
    private List<Double> trends;

    public List<Double> getTrends() { return trends; }

    private String crop;
    private java.util.List<String> labels;
    private Double currentAverage;
    private Double changePercent;
    private String insight;
    private int samples;

    public String getCrop() { return crop; }
    public java.util.List<String> getLabels() { return labels; }
    public Double getCurrentAverage() { return currentAverage; }
    public Double getChangePercent() { return changePercent; }
    public String getInsight() { return insight; }
    public int getSamples() { return samples; }
}
