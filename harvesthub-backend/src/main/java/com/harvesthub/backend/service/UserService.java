package com.harvesthub.backend.service;

import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User registerUser(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already exists");
        }
        if (userRepository.findByMobileNumber(user.getMobileNumber()).isPresent()) {
            throw new IllegalStateException("Mobile number already exists");
        }
        if (userRepository.findByNic(user.getNic()).isPresent()) {
            throw new IllegalStateException("NIC already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public Optional<User> findByEmailOrMobile(String identifier) {
        if (identifier == null || identifier.isBlank()) return Optional.empty();
        String trimmed = identifier.trim();
        Optional<User> user = userRepository.findByEmail(trimmed)
                .or(() -> userRepository.findByMobileNumber(trimmed))
                .or(() -> userRepository.findById(trimmed))
                .or(() -> userRepository.findByNic(trimmed));
        if (user.isPresent()) return user;

        return userRepository.findAll().stream()
                .filter(u -> (u.getEmail() != null && u.getEmail().split("@")[0].equalsIgnoreCase(trimmed))
                        || (u.getFullName() != null && u.getFullName().toLowerCase().startsWith(trimmed.toLowerCase()))
                        || (u.getId() != null && u.getId().toLowerCase().contains(trimmed.toLowerCase())))
                .findFirst();
    }

    public void createPasswordResetTokenForUser(String emailOrMobile) {
        if (emailOrMobile == null || emailOrMobile.isBlank()) {
            return;
        }
        Optional<User> userOptional = findByEmailOrMobile(emailOrMobile.trim());
        if (userOptional.isEmpty()) {
            return;
        }

        User user = userOptional.get();
        // Generate a 6-digit numerical OTP code
        String sixDigitOtp = String.format("%06d", new Random().nextInt(1000000));
        user.setResetPasswordToken(sixDigitOtp);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            emailService.sendPasswordResetEmail(user.getEmail(), sixDigitOtp);
        }
    }

    public void resetPassword(String token, String newPassword) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalStateException("Verification code is required.");
        }
        String cleanToken = token.trim();

        Optional<User> userOptional = userRepository.findByResetPasswordToken(cleanToken);
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findAll().stream()
                    .filter(u -> cleanToken.equalsIgnoreCase(u.getResetPasswordToken()))
                    .findFirst();
        }

        if (userOptional.isEmpty() || userOptional.get().getResetPasswordTokenExpiry() == null
                || userOptional.get().getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Invalid or expired 6-digit verification code.");
        }

        User user = userOptional.get();
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
    }
}