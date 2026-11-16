package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.WalletSummaryResponse;
import com.harvesthub.backend.model.Transaction;
import com.harvesthub.backend.security.CurrentUser;
import com.harvesthub.backend.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Mock wallet (proposal §4.2). Path ids are checked against the token. */
@RestController
public class WalletController {

    private final CurrentUser currentUser;
    private final WalletService walletService;

    public WalletController(CurrentUser currentUser, WalletService walletService) {
        this.currentUser = currentUser;
        this.walletService = walletService;
    }

    @GetMapping("wallet/{userId}/summary")
    public ResponseEntity<WalletSummaryResponse> getWalletSummary(@PathVariable String userId) {
        currentUser.requireSelfOrStaff(userId);
        return ResponseEntity.ok(walletService.getWalletSummary(userId));
    }

    @GetMapping("wallet/{userId}/transactions")
    public ResponseEntity<List<Transaction>> getRecentTransactions(
            @PathVariable String userId,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        currentUser.requireSelfOrStaff(userId);
        return ResponseEntity.ok(walletService.getRecentTransactions(userId, limit));
    }

    /** Mock top-up "Deposit Funds" (Figma buyer wallet): body {"amount": 5000}. Only the wallet owner. */
    @PostMapping("wallet/{userId}/deposit")
    public ResponseEntity<Transaction> deposit(@PathVariable String userId, @RequestBody Map<String, Object> body) {
        if (!currentUser.id().equals(userId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Only the owner can top up");
        }
        if (!(body.get("amount") instanceof Number amount)) throw new IllegalStateException("Enter an amount");
        return ResponseEntity.ok(walletService.topUp(userId, amount.doubleValue()));
    }

    /** Mock withdrawal: body {"amount": 5000}. Only the wallet owner; blocked when the balance is negative. */
    @PostMapping("wallet/{userId}/withdraw")
    public ResponseEntity<Transaction> withdraw(@PathVariable String userId, @RequestBody Map<String, Object> body) {
        if (!currentUser.id().equals(userId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Only the owner can withdraw");
        }
        if (!(body.get("amount") instanceof Number amount)) throw new IllegalStateException("Enter an amount");
        return ResponseEntity.ok(walletService.withdraw(userId, amount.doubleValue()));
    }
}
