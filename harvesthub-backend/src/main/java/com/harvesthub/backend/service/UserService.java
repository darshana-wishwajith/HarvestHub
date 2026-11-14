package com.harvesthub.backend.service;

import com.harvesthub.backend.dto.RegisterRequest;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.UserRepository;
import com.harvesthub.backend.security.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final int MAX_FAILED_LOGINS = 5;
    private static final Duration LOCKOUT = Duration.ofMinutes(15);
    private static final int MAX_RESET_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    /** identifier -> failed login tracking (in-memory; resets on restart, sufficient for a single instance). */
    private final Map<String, Attempts> failedLogins = new ConcurrentHashMap<>();
    /** userId -> wrong reset-code attempts for the currently issued code. */
    private final Map<String, Integer> resetAttempts = new ConcurrentHashMap<>();

    private static final class Attempts {
        int count;
        Instant lockedUntil = Instant.EPOCH;
    }

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /** true = value is free to use. Blank values are reported as available (nothing to check). */
    public java.util.Map<String, Boolean> checkAvailability(String email, String mobile, String nic) {
        java.util.Map<String, Boolean> out = new java.util.LinkedHashMap<>();
        out.put("email", email == null || email.isBlank()
                || userRepository.findByEmail(email.trim().toLowerCase()).isEmpty());
        out.put("mobileNumber", mobile == null || mobile.isBlank()
                || userRepository.findByMobileNumber(mobile.trim()).isEmpty());
        out.put("nic", nic == null || nic.isBlank()
                || userRepository.findByNic(nic.trim().toUpperCase()).isEmpty());
        return out;
    }

    public User registerUser(RegisterRequest req) {
        String role = req.getRole() == null || req.getRole().isBlank() ? Roles.FARMER : req.getRole().trim().toUpperCase();
        if (!Roles.SELF_REGISTERABLE.contains(role)) {
            throw new IllegalStateException("Role not allowed for self-registration");
        }
        if (Roles.FARMER.equals(role) && req.getFarmLocation() == null) {
            throw new IllegalStateException("Farm location is required for farmers");
        }
        if (req.getFarmLocation() != null) {
            double lat = req.getFarmLocation().getLatitude(), lng = req.getFarmLocation().getLongitude();
            // Sri Lanka bounding box (with a small margin) - rejects 0,0 and other unset / bogus pins.
            if (lat < 5.7 || lat > 10.1 || lng < 79.3 || lng > 82.1) {
                throw new IllegalStateException("Farm location must be inside Sri Lanka");
            }
        }
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Email already exists");
        }
        if (userRepository.findByMobileNumber(req.getMobileNumber()).isPresent()) {
            throw new IllegalStateException("Mobile number already exists");
        }
        if (userRepository.findByNic(req.getNic().toUpperCase()).isPresent()) {
            throw new IllegalStateException("NIC already exists");
        }
        User user = new User();
        user.setFullName(req.getFullName().trim());
        user.setEmail(email);
        user.setMobileNumber(req.getMobileNumber());
        user.setPhoneNumber(req.getMobileNumber());
        user.setNic(req.getNic().toUpperCase());
        user.setRole(role);
        user.setFarmLocation(req.getFarmLocation());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setPasswordLastUpdated(LocalDateTime.now().toString());
        return userRepository.save(user);
    }

    /** Exact lookup by email, mobile number or NIC. No partial / fuzzy matching. */
    public Optional<User> findByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) return Optional.empty();
        String id = identifier.trim();
        return userRepository.findByEmail(id.toLowerCase())
                .or(() -> userRepository.findByEmail(id))
                .or(() -> userRepository.findByMobileNumber(id))
                .or(() -> userRepository.findByNic(id.toUpperCase()));
    }

    /**
     * Verifies credentials with lockout after repeated failures.
     * Accounts created by the old Android build stored bcrypt(sha256(password)); those are verified once
     * and transparently re-hashed to bcrypt(password).
     */
    public Optional<User> authenticate(String identifier, String rawPassword) {
        String key = identifier == null ? "" : identifier.trim().toLowerCase();
        Attempts a = failedLogins.get(key);
        if (a != null && Instant.now().isBefore(a.lockedUntil)) {
            throw new IllegalStateException("Too many failed attempts. Try again in 15 minutes.");
        }
        Optional<User> userOpt = findByIdentifier(identifier);
        boolean ok = false;
        if (userOpt.isPresent() && rawPassword != null && userOpt.get().getPassword() != null) {
            User user = userOpt.get();
            String stored = user.getPassword();
            if (stored.startsWith("$2") && passwordEncoder.matches(rawPassword, stored)) {
                ok = true;
            } else if (stored.startsWith("$2") && passwordEncoder.matches(sha256Hex(rawPassword), stored)) {
                user.setPassword(passwordEncoder.encode(rawPassword)); // migrate legacy client-side hash
                userRepository.save(user);
                ok = true;
            }
        }
        if (!ok) {
            Attempts at = failedLogins.computeIfAbsent(key, k -> new Attempts());
            synchronized (at) {
                at.count++;
                if (at.count >= MAX_FAILED_LOGINS) {
                    at.lockedUntil = Instant.now().plus(LOCKOUT);
                    at.count = 0;
                }
            }
            return Optional.empty();
        }
        failedLogins.remove(key);
        return userOpt;
    }

    public void createPasswordResetTokenForUser(String identifier) {
        Optional<User> userOptional = findByIdentifier(identifier);
        if (userOptional.isEmpty()) {
            return; // don't reveal whether the account exists
        }
        User user = userOptional.get();
        String code = String.format("%06d", random.nextInt(1_000_000));
        user.setResetPasswordToken(passwordEncoder.encode(code)); // store only a hash of the code
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);
        resetAttempts.remove(user.getId());
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                emailService.sendPasswordResetEmail(user.getEmail(), code);
            } catch (Exception e) {
                log.warn("Password reset email could not be sent for user {}", user.getId());
            }
        }
    }

    public void resetPassword(String identifier, String code, String newPassword) {
        User user = findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalStateException("Invalid or expired verification code."));
        String stored = user.getResetPasswordToken();
        if (stored == null || !stored.startsWith("$2") || user.getResetPasswordTokenExpiry() == null
                || user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Invalid or expired verification code.");
        }
        if (!passwordEncoder.matches(code.trim(), stored)) {
            int attempts = resetAttempts.merge(user.getId(), 1, Integer::sum);
            if (attempts >= MAX_RESET_ATTEMPTS) { // burn the code after repeated wrong guesses
                user.setResetPasswordToken(null);
                user.setResetPasswordTokenExpiry(null);
                userRepository.save(user);
                resetAttempts.remove(user.getId());
            }
            throw new IllegalStateException("Invalid or expired verification code.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordLastUpdated(LocalDateTime.now().toString());
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
        resetAttempts.remove(user.getId());
    }

    private static String sha256Hex(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
