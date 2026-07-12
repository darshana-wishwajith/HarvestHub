package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.LoginRequest;
import com.harvesthub.backend.dto.LoginResponse;
import com.harvesthub.backend.dto.ResetPasswordRequest;
import com.harvesthub.backend.dto.UserDto;
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
@RequestMapping("/api/auth")
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

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        // 1. Authenticate the user
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmailOrMobile(), loginRequest.getPassword())
            );
        } catch (AuthenticationException e) {
            logger.warn("Authentication failed for user: {}. Reason: {}", loginRequest.getEmailOrMobile(), e.getMessage());
            Map<String, String> errorResponse = Collections.singletonMap("error", "Invalid credentials");
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }

        // 2. Find the user in the database
        Optional<User> userOptional = userService.findByEmailOrMobile(loginRequest.getEmailOrMobile());

        // 3. Handle the result with a simple if/else
        if (!userOptional.isPresent()) { // Corrected for older Java versions
            // This is a safeguard. If auth succeeded, the user must exist.
            logger.error("CRITICAL: User was authenticated but not found in the database: {}", loginRequest.getEmailOrMobile());
            Map<String, String> errorResponse = Collections.singletonMap("error", "An internal server error occurred.");
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            // 4. If user is found, create and return the JWT
            User user = userOptional.get();
            final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            final String jwt = jwtUtil.generateToken(userDetails);
            LoginResponse loginResponse = new LoginResponse(jwt, new UserDto(user.getId(), user.getEmail(), user.getRole()));
            return ResponseEntity.ok(loginResponse);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest resetRequest) {
        String userEmail = resetRequest.getResetToken();
        User user = userService.findByEmailOrMobile(userEmail)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body("Invalid reset token.");
        }

        user.setPassword(passwordEncoder.encode(resetRequest.getNewPassword()));
        userService.registerUser(user);

        return ResponseEntity.ok("Password has been reset successfully.");
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
