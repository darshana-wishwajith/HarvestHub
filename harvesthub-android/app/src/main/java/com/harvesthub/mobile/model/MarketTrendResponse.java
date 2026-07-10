package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MarketTrendResponse {
    @SerializedName("trends")
    private List<Double> trends;

    public List<Double> getTrends() { return trends; }
}
