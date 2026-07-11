package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class WalletSummaryResponse {
    @SerializedName("availableBalance")
    private String availableBalance;

    @SerializedName("trustScore")
    private String trustScore;

    @SerializedName("emptyTripPenalties")
    private String emptyTripPenalties;

    public WalletSummaryResponse(String availableBalance, String trustScore, String emptyTripPenalties) {
        this.availableBalance = availableBalance;
        this.trustScore = trustScore;
        this.emptyTripPenalties = emptyTripPenalties;
    }

    public String getAvailableBalance() { return availableBalance; }
    public String getTrustScore() { return trustScore; }
    public String getEmptyTripPenalties() { return emptyTripPenalties; }
}
