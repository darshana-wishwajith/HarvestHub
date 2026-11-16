package com.harvesthub.backend.dto;

import com.harvesthub.backend.model.FarmLocation;
import com.harvesthub.backend.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Public view of a user. Never contains password hashes, reset codes or NIC. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;
    private String email;
    private String role;
    private String fullName;
    private String mobileNumber;
    private FarmLocation farmLocation;
    // transporter only (null for other roles)
    private String vehicleType;
    private String vehiclePlate;
    private Double vehicleCapacityKg;
    private String vehicleBodyType;
    private Boolean online;
    private boolean licenceUploaded;

    public UserDto(String id, String email, String role, String fullName, String mobileNumber, FarmLocation farmLocation) {
        this(id, email, role, fullName, mobileNumber, farmLocation, null, null, null, null, null, false);
    }

    public static UserDto from(User u) {
        UserDto d = new UserDto(u.getId(), u.getEmail(), u.getRole(), u.getFullName(),
                u.getMobileNumber(), u.getFarmLocation());
        if ("TRANSPORTER".equals(u.getRole())) {
            d.setVehicleType(u.getVehicleType());
            d.setVehiclePlate(u.getVehiclePlate());
            d.setVehicleCapacityKg(u.getVehicleCapacityKg());
            d.setVehicleBodyType(u.getVehicleBodyType());
            d.setOnline(!Boolean.FALSE.equals(u.getOnline()));
            d.setLicenceUploaded(u.getLicenceUrl() != null);
        }
        return d;
    }
}
