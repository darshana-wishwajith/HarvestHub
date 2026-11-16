package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Mock wallet (proposal §4.2: no real gateway). One per user, plus "PLATFORM" (fee revenue)
 * and "ESCROW" (buyer money held for orders). Balance may go negative after a penalty
 * (workflow 5.3) — the user is then suspended until it is paid back.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "wallets")
public class Wallet {
    public static final String PLATFORM = "PLATFORM";
    public static final String ESCROW = "ESCROW";

    @Id
    private String id;
    @Indexed(unique = true)
    private String userId;
    private double balance;
    private double totalPenalties;   // Empty Trip Fees / damage charges paid
    private String updatedAt;

    public boolean isSuspended() {
        return balance < 0;
    }
}
