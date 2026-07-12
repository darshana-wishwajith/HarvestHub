package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class SendMessageRequest {
    @SerializedName("text")
    private String text;

    public SendMessageRequest(String text) {
        this.text = text;
    }

    public String getText() { return text; }
}
