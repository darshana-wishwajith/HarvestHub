package com.harvesthub.backend.service;

import com.harvesthub.backend.dto.GenericResponse;
import com.harvesthub.backend.dto.VerifyOtpResponse;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailService emailService;

    // Store OTP: identifier -> {otp, expiry}
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    private static class OtpEntry {
        String otp;
        LocalDateTime expiry;
        String userId;
        OtpEntry(String otp, LocalDateTime expiry, String userId) {
            this.otp = otp;
            this.expiry = expiry;
            this.userId = userId;
        }
    }

    public GenericResponse sendOtp(String identifier) {
        // Generate 6-digit OTP
        String otp = String.format("%06d", new SecureRandom().nextInt(999999));
        
        // Look up user by email or mobile
        var userOpt = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByMobileNumber(identifier));
        
        String userId = userOpt.map(User::getId).orElse(identifier);
        
        otpStore.put(identifier, new OtpEntry(otp, LocalDateTime.now().plusMinutes(10), userId));
        
        // Send OTP via email if it's an email
        if (identifier.contains("@")) {
            try {
                emailService.sendOtpEmail(identifier, otp);
            } catch (Exception e) {
                // Log but don't fail
            }
        }
        
        // For development, log the OTP
        System.out.println("OTP for " + identifier + ": " + otp);
        
        return new GenericResponse(true, "OTP sent successfully");
    }

    public GenericResponse resendOtp(String identifier) {
        return sendOtp(identifier); // Simply regenerate and resend
    }

    public VerifyOtpResponse verifyOtp(String userId, String otp) {
        // Search by userId or identifier
        for (Map.Entry<String, OtpEntry> entry : otpStore.entrySet()) {
            OtpEntry otpEntry = entry.getValue();
            if ((otpEntry.userId.equals(userId) || entry.getKey().equals(userId))
                    && otpEntry.otp.equals(otp)
                    && otpEntry.expiry.isAfter(LocalDateTime.now())) {
                otpStore.remove(entry.getKey());
                String resetToken = UUID.randomUUID().toString();
                return new VerifyOtpResponse(true, "OTP verified successfully", resetToken);
            }
        }
        return new VerifyOtpResponse(false, "Invalid or expired OTP", null);
    }
}
