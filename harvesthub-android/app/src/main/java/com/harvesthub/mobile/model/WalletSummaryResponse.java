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

    private double balance;
    private int trustPercent;
    private Double ratingAverage;
    private int ratingCount;
    private boolean suspended;
    private double totalPenalties;
    // buyer wallet (15 Nov)
    private double escrowHeld;
    private double escrowReleased;
    private double todayInflow;
    private int todayInflowCount;

    public double getEscrowHeld() { return escrowHeld; }
    public double getEscrowReleased() { return escrowReleased; }
    public double getTodayInflow() { return todayInflow; }
    public int getTodayInflowCount() { return todayInflowCount; }

    public double getBalance() { return balance; }
    public int getTrustPercent() { return trustPercent; }
    public Double getRatingAverage() { return ratingAverage; }
    public int getRatingCount() { return ratingCount; }
    public boolean isSuspended() { return suspended; }
    public double getTotalPenalties() { return totalPenalties; }
}
