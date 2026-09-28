package com.harvesthub.backend.service;

import com.harvesthub.backend.dto.FarmerProfileResponse;
import com.harvesthub.backend.model.FarmLocation;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FarmerProfileService {
    @Autowired
    private UserRepository userRepository;

    private static final String UPLOAD_DIR = "uploads/photos/";

    public FarmerProfileResponse getFarmerProfile(String farmerId) {
        User user = userRepository.findById(farmerId).orElse(null);
        if (user == null) return null;
        return new FarmerProfileResponse(
                user.getFullName(),
                user.getEmail(),
                user.getMobileNumber(),
                user.getNic(),
                user.getProfilePhotoUrl(),
                user.getPasswordLastUpdated()
        );
    }

    public boolean updateProfile(String farmerId, String fullName, String phoneNumber, String nic, MultipartFile photo) {
        User user = userRepository.findById(farmerId).orElse(null);
        if (user == null) return false;

        if (fullName != null) user.setFullName(fullName);
        if (phoneNumber != null) user.setMobileNumber(phoneNumber);
        if (nic != null) user.setNic(nic);

        if (photo != null && !photo.isEmpty()) {
            try {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                Files.createDirectories(uploadPath);
                String fileName = UUID.randomUUID() + "_" + photo.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(photo.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                user.setProfilePhotoUrl("/uploads/photos/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        userRepository.save(user);
        return true;
    }

    public void saveFarmLocation(String farmerId, double latitude, double longitude) {
        User user = userRepository.findById(farmerId).orElse(null);
        if (user != null) {
            FarmLocation location = user.getFarmLocation();
            if (location == null) {
                location = new FarmLocation();
            }
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            user.setFarmLocation(location);
            userRepository.save(user);
        }
    }
}
