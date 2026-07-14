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

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // Compatibility methods for existing RoleSelectionActivity code
    public String getName() { return email; }
}
