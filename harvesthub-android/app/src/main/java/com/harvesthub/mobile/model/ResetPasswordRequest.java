package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordRequest {
    /** Email (or mobile) the verification code was sent for. */
    @SerializedName("email")
    private String email;

    @SerializedName("resetToken")
    private String resetToken;

    @SerializedName("newPassword")
    private String newPassword;

    public ResetPasswordRequest(String email, String resetToken, String newPassword) {
        this.email = email;
        this.resetToken = resetToken;
        this.newPassword = newPassword;
    }

    public String getEmail() { return email; }
    public String getResetToken() { return resetToken; }
    public String getNewPassword() { return newPassword; }
}
