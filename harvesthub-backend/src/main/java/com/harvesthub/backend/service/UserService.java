package com.harvesthub.backend.service;

import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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


    public void createPasswordResetTokenForUser(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            // Don't reveal that the user doesn't exist
            return;
        }
        User user = userOptional.get();
        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1)); // Token is valid for 1 hour
        userRepository.save(user);
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    public void resetPassword(String token, String newPassword) {
        Optional<User> userOptional = userRepository.findByResetPasswordToken(token);
        if (userOptional.isEmpty() || userOptional.get().getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Invalid or expired password reset token.");
        }
        User user = userOptional.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
    }
}
