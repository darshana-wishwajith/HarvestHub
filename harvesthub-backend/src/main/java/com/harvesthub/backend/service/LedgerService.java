package com.harvesthub.backend.service;

import com.harvesthub.backend.model.Transaction;
import com.harvesthub.backend.model.Wallet;
import com.harvesthub.backend.repository.TransactionRepository;
import com.harvesthub.backend.repository.WalletRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mock wallet ledger. Every money movement is a debit on one wallet and a credit on another,
 * each recorded as a {@link Transaction}. Balances may go negative (penalties, workflow 5.3).
 */
@Service
public class LedgerService {

    static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WalletRepository wallets;
    private final TransactionRepository transactions;

    public LedgerService(WalletRepository wallets, TransactionRepository transactions) {
        this.wallets = wallets;
        this.transactions = transactions;
    }

    public Wallet wallet(String userId) {
        return wallets.findByUserId(userId).orElseGet(() -> {
            Wallet w = new Wallet();
            w.setUserId(userId);
            w.setUpdatedAt(now());
            return wallets.save(w);
        });
    }

    /** Moves money between two wallets and records both sides. Returns the receiver's transaction. */
    public synchronized Transaction transfer(String fromUserId, String toUserId, double amount,
                                             String type, String fromTitle, String toTitle, String orderId) {
        if (amount <= 0) return null;
        double value = FeeCalculator.round2(amount);
        if (fromUserId != null) post(fromUserId, -value, type, fromTitle, orderId);
        return toUserId == null ? null : post(toUserId, value, type, toTitle, orderId);
    }

    /** Money entering the system from the (mock) payment gateway, e.g. a buyer payment into escrow. */
    public Transaction deposit(String payerUserId, String toUserId, double amount, String type, String payerTitle,
                               String orderId) {
        double value = FeeCalculator.round2(amount);
        // the payer's wallet is not debited (they pay by card); we still record it for their history
        Transaction payerRecord = record(payerUserId, -value, wallet(payerUserId).getBalance(), type, payerTitle, orderId);
        post(toUserId, value, type, "Escrow received: " + payerTitle, orderId);
        return payerRecord;
    }

    /**
     * Buyer pays into escrow. CARD = simulated gateway (money enters the system); WALLET = debited from the
     * buyer's mock wallet, refused when the available balance is too low (user decision 15 Nov).
     */
    public Transaction payIntoEscrow(String payerUserId, String method, double amount, String type, String title,
                                     String orderId) {
        if ("WALLET".equalsIgnoreCase(method)) {
            Wallet w = wallet(payerUserId);
            if (w.isSuspended()) throw new IllegalStateException("Your wallet has a negative balance. Pay by card instead.");
            if (amount > w.getBalance() + 1e-6) {
                throw new IllegalStateException("Insufficient wallet balance. Available LKR " + format(w.getBalance())
                        + ", needed LKR " + format(amount) + ". Top up or pay by card.");
            }
            transfer(payerUserId, Wallet.ESCROW, amount, type, title + " (wallet)", "Escrow received: " + title, orderId);
            return null;
        }
        return deposit(payerUserId, Wallet.ESCROW, amount, type, title + " (card)", orderId);
    }

    /** Mock top-up from a simulated card (buyer wallet "Deposit Funds"). */
    public Transaction topUp(String userId, double amount) {
        if (amount <= 0) throw new IllegalStateException("Enter an amount greater than zero.");
        if (amount > 5_000_000) throw new IllegalStateException("The maximum top-up is LKR 5,000,000.");
        return post(userId, FeeCalculator.round2(amount), "TOP_UP", "Wallet top-up (simulated card)", null);
    }

    /** Money leaving the system (mock bank withdrawal). */
    public Transaction withdraw(String userId, double amount) {
        Wallet w = wallet(userId);
        if (w.isSuspended()) throw new IllegalStateException("Your wallet has a negative balance. Withdrawals are blocked.");
        if (amount <= 0) throw new IllegalStateException("Enter an amount greater than zero.");
        if (amount > w.getBalance() + 1e-6) throw new IllegalStateException("Amount is more than your available balance.");
        return post(userId, -FeeCalculator.round2(amount), "WITHDRAWAL", "Withdrawal to bank (mock)", null);
    }

    public void addPenalty(String userId, double amount) {
        Wallet w = wallet(userId);
        w.setTotalPenalties(FeeCalculator.round2(w.getTotalPenalties() + amount));
        wallets.save(w);
    }

    private Transaction post(String userId, double signedAmount, String type, String title, String orderId) {
        Wallet w = wallet(userId);
        w.setBalance(FeeCalculator.round2(w.getBalance() + signedAmount));
        w.setUpdatedAt(now());
        wallets.save(w);
        return record(userId, signedAmount, w.getBalance(), type, title, orderId);
    }

    private Transaction record(String userId, double signedAmount, double balanceAfter, String type, String title, String orderId) {
        Transaction t = new Transaction();
        t.setFarmerId(userId);
        t.setTitle(title);
        t.setTimestamp(now());
        t.setAmountValue(Math.abs(signedAmount));
        t.setAmount(format(Math.abs(signedAmount)));
        t.setCredit(signedAmount >= 0);
        t.setBalanceAfter(balanceAfter);
        t.setType(type);
        t.setOrderId(orderId);
        return transactions.save(t);
    }

    public static String format(double amount) {
        return Math.abs(amount - Math.rint(amount)) < 0.005
                ? String.format("%,.0f", amount) : String.format("%,.2f", amount);
    }

    static String now() {
        return LocalDateTime.now().format(TS);
    }
}
