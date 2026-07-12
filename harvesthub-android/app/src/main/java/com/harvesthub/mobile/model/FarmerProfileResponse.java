package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class FarmerProfileResponse {
    @SerializedName("fullName")
    private String fullName;

    @SerializedName("email")
    private String email;

    @SerializedName("phoneNumber")
    private String phoneNumber;

    @SerializedName("nic")
    private String nic;

    @SerializedName("profilePhotoUrl")
    private String profilePhotoUrl;

    @SerializedName("passwordLastUpdated")
    private String passwordLastUpdated;

    public FarmerProfileResponse(String fullName, String email, String phoneNumber, String nic, String profilePhotoUrl, String passwordLastUpdated) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.nic = nic;
        this.profilePhotoUrl = profilePhotoUrl;
        this.passwordLastUpdated = passwordLastUpdated;
    }

    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getNic() { return nic; }
    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public String getPasswordLastUpdated() { return passwordLastUpdated; }
}
