package com.harvesthub.backend.security;

/** Authenticated principal derived from a validated JWT (and confirmed against the DB). */
public record AuthUser(String id, String email, String role, String fullName) {
    public boolean isStaff() {
        return Roles.OFFICER.equals(role) || Roles.ADMIN.equals(role);
    }
}
