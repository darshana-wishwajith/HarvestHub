package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("token")
    private String token;

    @SerializedName("user")
    private User user;

    public String getToken() { return token; }
    public User getUser() { return user; }
}
