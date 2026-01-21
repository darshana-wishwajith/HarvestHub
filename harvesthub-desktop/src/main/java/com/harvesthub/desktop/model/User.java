package com.harvesthub.desktop.model;

public class User {
    private Long id;
    private String name;
    private String role; // Farmer, Buyer, Officer, Transporter

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}