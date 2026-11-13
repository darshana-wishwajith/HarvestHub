package com.harvesthub.backend.security;

import java.util.Set;

public final class Roles {
    public static final String FARMER = "FARMER";
    public static final String BUYER = "BUYER";
    public static final String TRANSPORTER = "TRANSPORTER";
    public static final String OFFICER = "OFFICER";
    public static final String ADMIN = "ADMIN";

    /** Roles a user may choose at self-registration. Officers and admins are provisioned by seed/admin only. */
    public static final Set<String> SELF_REGISTERABLE = Set.of(FARMER, BUYER, TRANSPORTER);

    private Roles() {}
}
