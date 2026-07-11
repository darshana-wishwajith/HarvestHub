package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class SendOtpRequest {
    @SerializedName("identifier")
    private String identifier;

    public SendOtpRequest(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
