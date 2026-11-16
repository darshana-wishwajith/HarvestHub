package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Wallet ledger entry. {@code farmerId} holds the owner's user id for every role (kept for app compatibility).
 * {@code amount} is the display string ("45,000"); {@code amountValue} the number.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class Transaction {
    @Id
    private String id;
    @Indexed
    private String farmerId;
    private String title;
    private String timestamp;
    private String amount;
    @com.fasterxml.jackson.annotation.JsonProperty("isCredit")
    private boolean isCredit;

    private double amountValue;
    private double balanceAfter;
    private String orderId;
    /** ADVANCE_PAYMENT, BALANCE_PAYMENT, CROP_ADVANCE, CROP_BALANCE, TRANSPORT_PAYOUT, PLATFORM_FEE,
     *  REFUND, EMPTY_TRIP_FEE, DAMAGE_CHARGE, WITHDRAWAL, ESCROW_IN, ESCROW_OUT */
    private String type;
}
