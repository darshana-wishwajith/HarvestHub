package com.harvesthub.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// This tells Jackson to ignore any extra fields the backend sends
// (like 'role' or 'name') if you aren't using them in this table yet.
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    private String id; // <-- CRITICAL: Change this from Long to String
    private String email;
    private String password;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}