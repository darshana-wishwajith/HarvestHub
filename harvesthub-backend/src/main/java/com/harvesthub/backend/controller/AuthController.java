package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.*;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.service.UserService;
import com.harvesthub.backend.util.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/api/auth", "/api"})
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user) {
        try {
            logger.info("Attempting to register user with email: {}", user.getEmail());
            User registeredUser = userService.registerUser(user);
            logger.info("User registered successfully with ID: {}", registeredUser.getId());
            return ResponseEntity.ok("User registered successfully with ID: " + registeredUser.getId());
        } catch (IllegalStateException e) {
            logger.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("An unexpected error occurred during registration for user with email: {}", user.getEmail(), e);
            return ResponseEntity.status(500).body("An unexpected error occurred.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmailOrMobile(), loginRequest.getPassword())
            );
        } catch (AuthenticationException e) {
            logger.warn("Authentication failed for user: {}. Reason: {}", loginRequest.getEmailOrMobile(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Invalid credentials"));
        }

        Optional<User> userOptional = userService.findByEmailOrMobile(loginRequest.getEmailOrMobile());

        if (!userOptional.isPresent()) {
            logger.error("CRITICAL: User was authenticated but not found in the database: {}", loginRequest.getEmailOrMobile());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "An internal server error occurred."));
        } else {
            User user = userOptional.get();
            final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            final String jwt = jwtUtil.generateToken(userDetails);
            LoginResponse loginResponse = new LoginResponse(jwt, new UserDto(user.getId(), user.getEmail(), user.getRole()));
            return ResponseEntity.ok(loginResponse);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        userService.createPasswordResetTokenForUser(forgotPasswordRequest.getEmail());
        // Always return a success message to prevent user enumeration attacks
        return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a password reset token has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest resetRequest) {
        try {
            userService.resetPassword(resetRequest.getToken(), resetRequest.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/farmer/{id}/profile")
    public ResponseEntity<?> getFarmerProfile(@PathVariable String id) {
        return userService.findByEmailOrMobile(id)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/darshana")
    public String test() { return "Hi, I'm Darshana from HarvestHub Server!"; }
}
