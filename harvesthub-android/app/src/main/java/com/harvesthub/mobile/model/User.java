package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    private String id;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("password")
    private String password;

    @SerializedName("role")
    private String role;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("mobileNumber")
    private String mobileNumber;

    @SerializedName("farmLocation")
    private FarmLocation farmLocation;

    public User() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public FarmLocation getFarmLocation() { return farmLocation; }
    public void setFarmLocation(FarmLocation farmLocation) { this.farmLocation = farmLocation; }

    public String getLocation() {
        if (farmLocation != null && farmLocation.getAddress() != null) {
            return farmLocation.getAddress();
        }
        return "Dambulla, LK";
    }

    // Compatibility methods for existing RoleSelectionActivity code
    public String getName() { return fullName != null ? fullName : email; }
}
