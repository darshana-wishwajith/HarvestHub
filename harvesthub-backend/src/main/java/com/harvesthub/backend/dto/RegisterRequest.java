package com.harvesthub.backend.dto;

import com.harvesthub.backend.model.FarmLocation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Self-registration payload. Only whitelisted fields can be set by the client (no id, role escalation, tokens). */
@Data
public class RegisterRequest {
    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be 10 digits")
    private String mobileNumber;

    @NotBlank(message = "NIC is required")
    @Pattern(regexp = "^(\\d{9}[VvXx]|\\d{12})$", message = "NIC must be 9 digits + V/X or 12 digits")
    private String nic;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be 8-128 characters")
    private String password;

    /** FARMER (default), BUYER or TRANSPORTER. */
    private String role;

    /** Required for farmers (registered farm GPS location). */
    @Valid
    private FarmLocation farmLocation;
}
