package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class NotificationItem {
    @SerializedName("id")
    private String id;
    
    @SerializedName("type")
    private String type; // DELIVERY_SUCCESS, PENALTY, LISTING_APPROVED, ACCOUNT_VERIFIED
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("body")
    private String body;
    
    @SerializedName("timestamp")
    private String timestamp;
    
    @SerializedName("isRead")
    private boolean isRead;

    public NotificationItem(String id, String type, String title, String body, String timestamp, boolean isRead) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.body = body;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
