package com.harvesthub.backend.config;

import com.harvesthub.backend.model.FarmLocation;
import com.harvesthub.backend.model.Listing;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.ListingRepository;
import com.harvesthub.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            seedOfficerUsers();
            seedVerificationListings();
        } catch (Exception e) {
            logger.warn("Data initialization encountered an exception (continuing gracefully): {}", e.getMessage());
        }
    }

    private void seedOfficerUsers() {
        // Ensure default demo officer 'lahiru' exists
        if (userRepository.findByEmail("lahiru@harvesthub.lk").isEmpty()) {
            User lahiru = new User();
            lahiru.setId("user_officer_lahiru");
            lahiru.setFullName("Lahiru Sahan");
            lahiru.setEmail("lahiru@harvesthub.lk");
            lahiru.setMobileNumber("0712345679");
            lahiru.setPhoneNumber("0712345679");
            lahiru.setNic("951234567V");
            lahiru.setPassword(passwordEncoder.encode("officer123"));
            lahiru.setRole("OFFICER");
            lahiru.setFarmLocation(new FarmLocation("Agrarian Service Center, Peradeniya", 7.2618, 80.5975));
            userRepository.save(lahiru);
            logger.info("Initialized default officer user: lahiru@harvesthub.lk (password: officer123)");
        }

        // Ensure dilani officer has correct password for testing
        userRepository.findByEmail("dilani.silva@doa.gov.lk").ifPresent(user -> {
            if (!passwordEncoder.matches("password123", user.getPassword()) && !passwordEncoder.matches("officer123", user.getPassword())) {
                user.setPassword(passwordEncoder.encode("password123"));
                userRepository.save(user);
            }
        });
    }

    private void seedVerificationListings() {
        if (listingRepository.count() == 0) {
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            List<Listing> sampleListings = List.of(
                    Listing.builder()
                            .id("listing_paddy_01")
                            .farmerId("user_farmer_kamal")
                            .farmerName("Kalinga Perera")
                            .cropName("Paddy")
                            .variety("Bg 300")
                            .location("Anuradhapura North Sector")
                            .quantity("450 kg")
                            .price("Rs. 120/kg")
                            .totalValue("Rs. 54,000")
                            .status("PENDING")
                            .createdDate(now)
                            .updatedTime(now)
                            .gpsCoordinates("8.3114° N, 80.4037° E")
                            .deviceModel("Android CropScanner v4.2")
                            .referenceCode("LIST-8841")
                            .variancePercent("-1.1% dev")
                            .mediaFileCount(3)
                            .mediaCaption1("Wide Angle - Field View")
                            .mediaCaption2("Close-Up - Sample Grain")
                            .videoLabel("360° Field & Sample Video (10s)")
                            .build(),

                    Listing.builder()
                            .id("listing_cinnamon_02")
                            .farmerId("user_farmer_kamal")
                            .farmerName("Anura Silva")
                            .cropName("Cinnamon")
                            .variety("Sri Lankan Alba")
                            .location("Galle Central Hub")
                            .quantity("120 kg")
                            .price("Rs. 540/kg")
                            .totalValue("Rs. 64,800")
                            .status("PENDING")
                            .createdDate(now)
                            .updatedTime(now)
                            .gpsCoordinates("6.0535° N, 80.2210° E")
                            .deviceModel("Android CropScanner v4.2")
                            .referenceCode("LIST-5120")
                            .variancePercent("0.0% dev")
                            .mediaFileCount(3)
                            .mediaCaption1("Bark Quality Inspection")
                            .mediaCaption2("Bundle Density Measurement")
                            .videoLabel("Bark Peel Inspection (12s)")
                            .build(),

                    Listing.builder()
                            .id("listing_tea_03")
                            .farmerId("user_farmer_sunil")
                            .farmerName("Bandara Wijesinghe")
                            .cropName("Tea")
                            .variety("Nuwara Eliya Pekoe")
                            .location("Nuwara Eliya East")
                            .quantity("800 kg")
                            .price("Rs. 165/kg")
                            .totalValue("Rs. 132,000")
                            .status("PENDING")
                            .createdDate(now)
                            .updatedTime(now)
                            .gpsCoordinates("6.9497° N, 80.7891° E")
                            .deviceModel("iOS CropScanner v3.1")
                            .referenceCode("LIST-3912")
                            .variancePercent("+0.5% dev")
                            .mediaFileCount(3)
                            .mediaCaption1("Highland Flush Plot")
                            .mediaCaption2("Two Leaves & Bud Freshness")
                            .videoLabel("Plucking Quality Inspection (15s)")
                            .build(),

                    Listing.builder()
                            .id("listing_onion_04")
                            .farmerId("user_farmer_kamal")
                            .farmerName("Priyantha Kumara")
                            .cropName("Big Onions")
                            .variety("Dambulla Red")
                            .location("Dambulla Town")
                            .quantity("2,500 kg")
                            .price("Rs. 250/kg")
                            .totalValue("Rs. 625,000")
                            .status("PENDING")
                            .createdDate(now)
                            .updatedTime(now)
                            .gpsCoordinates("7.8731° N, 80.7718° E")
                            .deviceModel("Android CropScanner v4.2")
                            .referenceCode("LIST-2041")
                            .variancePercent("-0.4% dev")
                            .mediaFileCount(3)
                            .mediaCaption1("Crate Sorting Line")
                            .mediaCaption2("Bulb Firmness & Moisture Check")
                            .videoLabel("Bag Storage Inspection (10s)")
                            .build()
            );

            listingRepository.saveAll(sampleListings);
            logger.info("Seeded initial verification queue listings.");
        }
    }
}
