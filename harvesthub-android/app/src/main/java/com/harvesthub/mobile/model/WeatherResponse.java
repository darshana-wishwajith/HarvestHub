package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class WeatherResponse {
    @SerializedName("temperature")
    private String temperature;
    @SerializedName("condition")
    private String condition;
    @SerializedName("location")
    private String location;

    public String getTemperature() { return temperature; }
    public String getCondition() { return condition; }
    public String getLocation() { return location; }
}
