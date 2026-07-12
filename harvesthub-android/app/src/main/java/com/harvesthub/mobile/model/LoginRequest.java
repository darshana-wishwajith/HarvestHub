package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("emailOrMobile")
    private String emailOrMobile;

    @SerializedName("password")
    private String password;

    public LoginRequest(String emailOrMobile, String password) {
        this.emailOrMobile = emailOrMobile;
        this.password = password;
    }

    public String getEmailOrMobile() { return emailOrMobile; }
    public String getPassword() { return password; }
}
