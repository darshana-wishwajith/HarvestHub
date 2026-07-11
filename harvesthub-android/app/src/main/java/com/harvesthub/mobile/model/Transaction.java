package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class Transaction {
    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("amount")
    private String amount;

    @SerializedName("isCredit")
    private boolean isCredit;

    public Transaction(String id, String title, String timestamp, String amount, boolean isCredit) {
        this.id = id;
        this.title = title;
        this.timestamp = timestamp;
        this.amount = amount;
        this.isCredit = isCredit;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getTimestamp() { return timestamp; }
    public String getAmount() { return amount; }
    public boolean isCredit() { return isCredit; }
}
