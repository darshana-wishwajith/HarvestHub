package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordRequest {
    @SerializedName("resetToken")
    private String resetToken;

    @SerializedName("newPassword")
    private String newPassword;

    public ResetPasswordRequest(String resetToken, String newPassword) {
        this.resetToken = resetToken;
        this.newPassword = newPassword;
    }

    public String getResetToken() { return resetToken; }
    public String getNewPassword() { return newPassword; }
}
