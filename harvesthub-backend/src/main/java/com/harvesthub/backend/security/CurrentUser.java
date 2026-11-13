package com.harvesthub.backend.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/**
 * Single place to obtain the acting user. Controllers must use this instead of trusting
 * user ids that arrive in paths, query strings or request bodies.
 */
@Component
public class CurrentUser {

    public AuthUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user) {
            return user;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    public String id() {
        return get().id();
    }

    public boolean isStaff() {
        return get().isStaff();
    }

    /** Allows the call only for the user themselves, or for officers/admins. */
    public void requireSelfOrStaff(String userId) {
        AuthUser user = get();
        if (!user.isStaff() && !Objects.equals(user.id(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to access another user's data");
        }
    }

    /** Allows the call when the acting user is one of the given participants, or staff. */
    public void requireParticipantOrStaff(String... participantIds) {
        AuthUser user = get();
        if (user.isStaff()) return;
        for (String p : participantIds) {
            if (p != null && p.equals(user.id())) return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a participant of this resource");
    }

    public void requireRole(String... roles) {
        String r = get().role();
        for (String role : roles) {
            if (role.equals(r)) return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role not permitted");
    }
}
