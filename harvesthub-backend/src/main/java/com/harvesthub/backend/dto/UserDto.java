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

    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getRole(), u.getFullName(),
                u.getMobileNumber(), u.getFarmLocation());
    }
}
