package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.LoginRequest;
import com.harvesthub.backend.dto.LoginResponse;
import com.harvesthub.backend.dto.ResetPasswordRequest;
import com.harvesthub.backend.dto.UserDto;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.service.UserService;
import com.harvesthub.backend.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth") // Restored the full path
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
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            logger.info("Attempting to register user with email: {}", user.getEmail());
            User registeredUser = userService.registerUser(user);
            logger.info("User registered successfully with ID: {}", registeredUser.getId());
            return ResponseEntity.ok("User registered successfully with ID: " + registeredUser.getId());
        } catch (Exception e) {
            logger.error("Registration failed for user with email: {}", user.getEmail(), e);
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmailOrMobile(), loginRequest.getPassword())
        );

        final UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmailOrMobile());
        final User user = userService.findByEmailOrMobile(loginRequest.getEmailOrMobile()).get();
        final String jwt = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(new LoginResponse(jwt, new UserDto(user.getId(), user.getEmail(), user.getRole())));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest resetRequest) {
        // This is a simplified and insecure example.
        // A proper implementation would involve generating a token, sending it via email/SMS,
        // and then verifying it here.
        String userEmail = resetRequest.getResetToken();
        User user = userService.findByEmailOrMobile(userEmail)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body("Invalid reset token.");
        }

        user.setPassword(passwordEncoder.encode(resetRequest.getNewPassword()));
        userService.registerUser(user); // Re-using registerUser to save the updated user

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
