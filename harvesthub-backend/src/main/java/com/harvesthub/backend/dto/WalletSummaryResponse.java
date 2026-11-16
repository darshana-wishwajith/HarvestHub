package com.harvesthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Wallet screen (Figma 18). String fields are kept for older app builds. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletSummaryResponse {
    private String availableBalance;      // "125,000"
    private String trustScore;            // "94" (percent)
    private String emptyTripPenalties;    // "0"
    private double balance;
    private int trustPercent;
    private Double ratingAverage;         // null until rated
    private int ratingCount;
    private boolean suspended;            // negative balance (workflow 5.3)
    private double totalPenalties;
    // buyer wallet (Figma buyer Wallet 567:682)
    private double escrowHeld;            // buyer money locked in escrow for open orders
    private double escrowReleased;        // paid for completed orders
    private double todayInflow;           // credits received today
    private int todayInflowCount;
}
