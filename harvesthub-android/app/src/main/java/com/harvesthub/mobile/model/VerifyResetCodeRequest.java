package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

/** Body of POST api/auth/verify-reset-code. */
public class VerifyResetCodeRequest {
    /** Email (or mobile) the code was requested for. */
    @SerializedName("email")
    private final String email;

    @SerializedName("code")
    private final String code;

    public VerifyResetCodeRequest(String email, String code) {
        this.email = email;
        this.code = code;
    }

    public String getEmail() { return email; }
    public String getCode() { return code; }
}
