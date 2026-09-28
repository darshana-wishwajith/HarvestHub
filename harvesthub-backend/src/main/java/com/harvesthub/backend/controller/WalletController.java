package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.WalletSummaryResponse;
import com.harvesthub.backend.model.Transaction;
import com.harvesthub.backend.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WalletController {
    @Autowired
    private WalletService walletService;

    @GetMapping("wallet/{farmerId}/summary")
    public ResponseEntity<WalletSummaryResponse> getWalletSummary(@PathVariable String farmerId) {
        return ResponseEntity.ok(walletService.getWalletSummary(farmerId));
    }

    @GetMapping("wallet/{farmerId}/transactions")
    public ResponseEntity<List<Transaction>> getRecentTransactions(
            @PathVariable String farmerId,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(walletService.getRecentTransactions(farmerId, limit));
    }
}
