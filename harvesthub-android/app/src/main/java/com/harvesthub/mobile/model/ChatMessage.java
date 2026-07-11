package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class ChatMessage {
    @SerializedName("id")
    private String id;
    
    @SerializedName("senderId")
    private String senderId;
    
    @SerializedName("text")
    private String text;
    
    @SerializedName("timestamp")
    private String timestamp;
    
    @SerializedName("isSentByMe")
    private boolean isSentByMe;
    
    @SerializedName("isRead")
    private boolean isRead;

    public ChatMessage(String id, String senderId, String text, String timestamp, boolean isSentByMe, boolean isRead) {
        this.id = id;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
        this.isSentByMe = isSentByMe;
        this.isRead = isRead;
    }

    public String getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getText() { return text; }
    public String getTimestamp() { return timestamp; }
    public boolean isSentByMe() { return isSentByMe; }
    public boolean isRead() { return isRead; }
}
