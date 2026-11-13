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

    @org.springframework.beans.factory.annotation.Value("${harvesthub.seed-demo-data:false}")
    private boolean seedDemoData;

    @org.springframework.beans.factory.annotation.Value("${harvesthub.demo-password:}")
    private String demoPassword;

    @Override
    public void run(String... args) {
        if (!seedDemoData) {
            return; // production: never seed or modify data automatically
        }
        try {
            seedDemoUsers();
            seedVerificationListings();
        } catch (Exception e) {
            logger.warn("Data initialization encountered an exception (continuing gracefully): {}", e.getMessage());
        }
    }

    /**
     * Creates one demo account per role if it does not exist yet. Passwords come from DEMO_PASSWORD;
     * nothing is created without it, and existing accounts are never modified.
     */
    private void seedDemoUsers() {
        if (demoPassword == null || demoPassword.length() < 8) {
            logger.info("DEMO_PASSWORD not set (min 8 chars); skipping demo accounts.");
            return;
        }
        createIfMissing("user_officer_lahiru", "Lahiru Sahan", "officer@harvesthub.lk", "0712345679", "951234567V", "OFFICER",
                new FarmLocation("Agrarian Service Center, Peradeniya", 7.2618, 80.5975));
        createIfMissing("user_admin", "HarvestHub Admin", "admin@harvesthub.lk", "0712345670", "901234567V", "ADMIN", null);
        createIfMissing("user_farmer_kamal", "Kamal Perera", "farmer@harvesthub.lk", "0771234567", "881234567V", "FARMER",
                new FarmLocation("Dambulla, Matale", 7.8731, 80.6512));
        createIfMissing("user_buyer_nimal", "Nimal Fernando", "buyer@harvesthub.lk", "0761234567", "871234567V", "BUYER",
                new FarmLocation("Pettah, Colombo 11", 6.9375, 79.8500));
        createIfMissing("user_transporter_priyantha", "Priyantha Kumara", "transporter@harvesthub.lk", "0751234567", "861234567V", "TRANSPORTER",
                new FarmLocation("Kurunegala", 7.4863, 80.3647));
    }

    private void createIfMissing(String id, String name, String email, String mobile, String nic, String role, FarmLocation loc) {
        if (userRepository.existsById(id) || userRepository.findByEmail(email).isPresent()) {
            return;
        }
        User u = new User();
        u.setId(id);
        u.setFullName(name);
        u.setEmail(email);
        u.setMobileNumber(mobile);
        u.setPhoneNumber(mobile);
        u.setNic(nic);
        u.setRole(role);
        u.setFarmLocation(loc);
        u.setPassword(passwordEncoder.encode(demoPassword));
        userRepository.save(u);
        logger.info("Created demo {} account {}", role, email);
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
