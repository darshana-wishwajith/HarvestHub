package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class SendMessageRequest {
    @SerializedName("senderId")
    private String senderId;

    @SerializedName("text")
    private String text;

    public SendMessageRequest(String text) {
        this.text = text;
    }

    public SendMessageRequest(String senderId, String text) {
        this.senderId = senderId;
        this.text = text;
    }

    public String getSenderId() { return senderId; }
    public String getText() { return text; }
}
