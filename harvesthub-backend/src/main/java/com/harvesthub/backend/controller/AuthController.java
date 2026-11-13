package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.ForgotPasswordRequest;
import com.harvesthub.backend.dto.LoginRequest;
import com.harvesthub.backend.dto.LoginResponse;
import com.harvesthub.backend.dto.OfficerDto;
import com.harvesthub.backend.dto.RegisterRequest;
import com.harvesthub.backend.dto.ResetPasswordRequest;
import com.harvesthub.backend.dto.UserDto;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.security.Roles;
import com.harvesthub.backend.service.UserService;
import com.harvesthub.backend.util.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/api/auth", "/api"})
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        try {
            User registered = userService.registerUser(request);
            logger.info("Registered user {} with role {}", registered.getId(), registered.getRole());
            return ResponseEntity.ok(Map.of("message", "User registered successfully", "id", registered.getId()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        String identifier = loginRequest.getEmailOrMobile();
        if (identifier == null || identifier.isBlank() || loginRequest.getPassword() == null
                || loginRequest.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email/mobile and password are required"));
        }
        Optional<User> userOpt;
        try {
            userOpt = userService.authenticate(identifier, loginRequest.getPassword());
        } catch (IllegalStateException locked) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", locked.getMessage()));
        }
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
        User user = userOpt.get();
        String jwt = jwtUtil.generateToken(user);
        OfficerDto officerDto = null;
        if (Roles.OFFICER.equals(user.getRole()) || Roles.ADMIN.equals(user.getRole())) {
            officerDto = OfficerController.toOfficerDto(user);
        }
        return ResponseEntity.ok(new LoginResponse(jwt, UserDto.from(user), officerDto));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.createPasswordResetTokenForUser(request.getEmail());
        return ResponseEntity.ok(Map.of("message",
                "If an account with that email exists, a verification code has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            userService.resetPassword(request.getEmail(), request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
