package com.harvesthub.backend.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String id;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be 10 digits")
    @Indexed(unique = true, name = "mobileNumber_1")
    private String mobileNumber;

    @NotBlank(message = "NIC is required")
    @Indexed(unique = true, name = "nic_1")
    private String nic;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Indexed(unique = true, name = "email_1")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String role = "FARMER";

    @NotNull(message = "Farm location is required")
    @Valid
    private FarmLocation farmLocation;

    // Fields for password reset
    private String resetPasswordToken;
    private LocalDateTime resetPasswordTokenExpiry;

    private String profilePhotoUrl;
    private String passwordLastUpdated;
    private String phoneNumber; // alias for mobileNumber for API compatibility

    // ---- Trust, maintained by TrustService ----
    private Double ratingAverage;      // 1..5, null until rated
    private Integer ratingCount;
    private Integer completedOrders;
    private Integer failedOrders;      // disputes caused, cancellations
    private Integer trustScore;        // 0..100 %

    // ---- Transporter vehicle + availability ----
    private String vehicleType;        // THREE_WHEELER, MINI_LORRY, LORRY, TRUCK
    private String vehiclePlate;
    private Double vehicleCapacityKg;
    private Boolean available;         // transporter has no active job
    private Boolean online;            // driver's own online/offline switch; null = online
    private String vehicleBodyType;    // REEFER | FLATBED | BOX – label only, not used in dispatch
    private String licenceUrl;         // driving-licence photo, stored for officers
    private String licenceUploadedAt;
    private Double lastLatitude;
    private Double lastLongitude;

    // ---- Settings ----
    private java.util.Map<String, Boolean> notificationPreferences;
    private String language;           // "en" only for now

    // ---- Account suspension by an Agricultural Officer ----
    // Separate from the wallet suspension. Blocks login and every API call.
    private Boolean accountSuspended;
    private String suspensionReason;
    private String suspendedAt;
    private String suspendedById;
    private String suspendedByName;
    private String reactivatedAt;

    public boolean isAccountSuspended() {
        return Boolean.TRUE.equals(accountSuspended);
    }
}
