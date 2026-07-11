package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class VerifyOtpResponse extends GenericResponse {
    @SerializedName("resetToken")
    private String resetToken;

    public String getResetToken() { return resetToken; }
}
