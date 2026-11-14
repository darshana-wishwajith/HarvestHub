package com.harvesthub.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** Body of POST /api/auth/verify-reset-code (forgot-password step 2). */
@Data
public class VerifyResetCodeRequest {
    /** Email (or mobile) the code was requested for. */
    @NotBlank(message = "Email or mobile number is required")
    private String email;

    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "\\s*\\d{6}\\s*", message = "Verification code must be 6 digits")
    @JsonAlias({"token", "resetToken"})
    private String code;
}
