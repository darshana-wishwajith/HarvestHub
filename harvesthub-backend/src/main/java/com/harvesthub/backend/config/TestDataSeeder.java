package com.harvesthub.backend.config;

import com.harvesthub.backend.dto.ConfirmDealRequest;
import com.harvesthub.backend.dto.TransporterEventRequest;
import com.harvesthub.backend.model.*;
import com.harvesthub.backend.repository.*;
import com.harvesthub.backend.service.OrderService;
import com.harvesthub.backend.service.TrustService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Repeatable TEST data for the shared Atlas database.
 * Runs only with the "seedtest" profile and then exits:
 * <pre>mvnw spring-boot:run -Dspring-boot.run.profiles=seedtest -Dspring-boot.run.arguments=--server.port=0</pre>
 * - Never updates or deletes existing documents that it did not create (all users use @test.harvesthub.lk).
 * - Idempotent: skips everything if the test farmer already exists (market prices are checked one by one).
 * - Orders are driven through the real OrderService so money, notifications and statuses are consistent.
 * - Passwords for all test accounts = DEMO_PASSWORD (harvesthub.demo-password).
 */
@Component
@Profile("seedtest")
public class TestDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TestDataSeeder.class);
    static final String FARMER_EMAIL = "nimal.farmer@test.harvesthub.lk";
    static final String BUYER_VIEW_MARKER = "farmer_kamala_test";
    /** Created by the transporter-view phase; its presence means that phase already ran. */
    static final String TRANSPORTER_VIEW_MARKER = "transporter_tharindu";
    /** Second Agricultural Officer for the desktop portal – shows the claim lock between two officers. */
    static final String OFFICER_VIEW_MARKER = "officer_nadeesha_test";

    private final UserRepository users;
    private final ListingRepository listings;
    private final WantedAdRepository wantedAds;
    private final SupplyOfferRepository offers;
    private final ChatMessageRepository chats;
    private final MarketPriceRepository prices;
    private final OrderRepository orders;
    private final TransactionRepository transactions;
    private final OrderService orderService;
    private final TrustService trust;
    private final PasswordEncoder encoder;
    private final ConfigurableApplicationContext context;
    private final String demoPassword;

    // buyer view
    @org.springframework.beans.factory.annotation.Autowired
    private LogisticsRepository logisticsRecords;
    @org.springframework.beans.factory.annotation.Autowired
    private com.harvesthub.backend.service.LedgerService ledger;
    /** Uploaded photos reused as pre-loading evidence (real files under /uploads). */
    private List<String> evidencePhotos = List.of();

    public TestDataSeeder(UserRepository users, ListingRepository listings, WantedAdRepository wantedAds,
                          SupplyOfferRepository offers, ChatMessageRepository chats, MarketPriceRepository prices,
                          OrderRepository orders, TransactionRepository transactions, OrderService orderService,
                          TrustService trust, PasswordEncoder encoder, ConfigurableApplicationContext context,
                          @Value("${harvesthub.demo-password:}") String demoPassword) {
        this.users = users;
        this.listings = listings;
        this.wantedAds = wantedAds;
        this.offers = offers;
        this.chats = chats;
        this.prices = prices;
        this.orders = orders;
        this.transactions = transactions;
        this.orderService = orderService;
        this.trust = trust;
        this.encoder = encoder;
        this.context = context;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        int code = 0;
        try {
            if (demoPassword == null || demoPassword.length() < 8) {
                throw new IllegalStateException("Set DEMO_PASSWORD (8+ chars) in application-local.properties first");
            }
            int priceCount = seedMarketPrices();
            log.info("SEED market_prices: {} new records", priceCount);
            if (users.findByEmail(FARMER_EMAIL).isPresent()) {
                log.info("SEED: test accounts already exist – skipping users, listings and orders");
            } else {
                seedAccountsAndActivity();
            }
            if (users.existsById(BUYER_VIEW_MARKER)) {
                log.info("SEED: buyer-view data already exists – skipping");
            } else {
                seedBuyerView();
            }
            if (users.existsById(TRANSPORTER_VIEW_MARKER)) {
                log.info("SEED: transporter-view data already exists – skipping");
            } else {
                seedTransporterView();
            }
            if (users.existsById(OFFICER_VIEW_MARKER)) {
                log.info("SEED: officer-view account already exists – skipping");
            } else {
                seedOfficerView();
            }
            log.info("SEED: done");
        } catch (Exception e) {
            log.error("SEED failed: {}", e.getMessage(), e);
            code = 1;
        }
        int exit = code;
        SpringApplication.exit(context, () -> exit);
        System.exit(exit);
    }

    // ------------------------------------------------------------------ users

    private User user(String id, String name, String email, String mobile, String role,
                      double lat, double lng, String address) {
        if (users.existsById(id)) throw new IllegalStateException("User id " + id + " already exists – refusing to modify it");
        User u = new User();
        u.setId(id);
        u.setFullName(name);
        u.setEmail(email);
        u.setMobileNumber(mobile);
        u.setPhoneNumber(mobile);
        u.setNic("TEST" + mobile);
        u.setPassword(encoder.encode(demoPassword));
        u.setPasswordLastUpdated(LocalDateTime.now().toString());
        u.setRole(role);
        FarmLocation loc = new FarmLocation();
        loc.setAddress(address);
        loc.setLatitude(lat);
        loc.setLongitude(lng);
        u.setFarmLocation(loc);
        return users.save(u);
    }

    private User driver(String id, String name, String mobile, String type, double capacity, String plate,
                        double lat, double lng, String base) {
        User d = user(id, name, id.replace('_', '.') + "@test.harvesthub.lk", mobile, "TRANSPORTER", lat, lng, base);
        d.setVehicleType(type);
        d.setVehicleCapacityKg(capacity);
        d.setVehiclePlate(plate);
        d.setAvailable(true);
        return users.save(d);
    }

    private void seedAccountsAndActivity() {
        // Buyers – ids reuse the ones existing orders / wanted ads already point at
        User roshan = user("user_buyer_roshan", "Roshan Silva", "roshan.buyer@test.harvesthub.lk", "0700000101",
                "BUYER", 6.9271, 79.8612, "Pettah, Colombo 11");
        User keells = user("buyer_keells", "Keells Supermarkets", "keells.buyer@test.harvesthub.lk", "0700000102",
                "BUYER", 6.9020, 79.8590, "Keells DC, Colombo 05");
        User greenmart = user("buyer_greenmart", "GreenMart Ltd", "greenmart.buyer@test.harvesthub.lk", "0700000103",
                "BUYER", 7.2906, 80.6337, "GreenMart, Kandy");
        // Transporters around the North Central / Central provinces
        User sunil = driver("transporter_sunil", "Sunil Perera", "0700000201", "LORRY", 4000, "WP LC-4589", 7.8600, 80.6500, "Dambulla");
        driver("transporter_samuel", "Samuel Ovitigala", "0700000202", "TRUCK", 10000, "WP LG-5582", 7.4863, 80.3647, "Kurunegala");
        driver("transporter_ruwan", "Ruwan Jayasinghe", "0700000203", "MINI_LORRY", 1000, "NC LB-2231", 8.3114, 80.4037, "Anuradhapura");
        driver("transporter_kasun", "Kasun Fernando", "0700000204", "THREE_WHEELER", 500, "CP QA-7710", 7.9403, 80.7011, "Habarana");
        // Test farmer with a full history
        User nimal = user("farmer_nimal_test", "Nimal Bandara", FARMER_EMAIL, "0700000301", "FARMER",
                7.8742, 80.6511, "Kandalama Road, Dambulla");

        // Listings in every My Listings state
        listing(nimal, "Carrots", 300, 220, "PENDING", null, null);
        listing(nimal, "Leeks", 200, 210, "UNDER_REVIEW", null, null);
        listing(nimal, "Potatoes", 450, 190, "RETAKE_REQUESTED", null, "Close-up photo is blurry – please retake in daylight.");
        listing(nimal, "Cabbage", 400, 160, "REJECTED", null, "Visible pest damage on most heads.");
        Listing onionsForOffer = listing(nimal, "Big Onions", 1200, 250, "ACTIVE", "A", null);
        listing(nimal, "Tomatoes", 500, 180, "ACTIVE", "B", null);

        // Wanted ads
        WantedAd onionAd = wanted(roshan, "Big Onions", "Dambulla Sector", 1000, 280, true);
        wanted(keells, "Tomatoes", "Colombo Sector", 500, 150, false);
        wanted(greenmart, "Potatoes", "Nuwara Eliya Sector", 2500, 190, false);
        // A pending supply offer from the farmer
        SupplyOffer offer = new SupplyOffer();
        offer.setId("offer_test_" + System.currentTimeMillis());
        offer.setRequestId(onionAd.getRequestId());
        offer.setBatchId(onionsForOffer.getId());
        offer.setFarmerId(nimal.getId());
        offer.setFarmerName(nimal.getFullName());
        offer.setCropName("Big Onions");
        offer.setGrade("A");
        offer.setLocation(nimal.getFarmLocation().getAddress());
        offer.setQuantity(500);
        offer.setPricePerKg(270);
        offer.setStatus("PENDING");
        offer.setCreatedDate(LocalDateTime.now().toString());
        offers.save(offer);

        // Orders at different stages, all through the real workflow
        String completed1 = runOrder(nimal, roshan, "Big Onions", 500, 250, "COMPLETED", 40);
        String completed2 = runOrder(nimal, greenmart, "Potatoes", 850, 185, "COMPLETED", 25);
        runOrder(nimal, keells, "Tomatoes", 300, 175, "IN_TRANSIT", 0);
        runOrder(nimal, greenmart, "Carrots", 600, 215, "TRANSPORTER_ACCEPTED", 0);
        runOrder(nimal, roshan, "Leeks", 250, 205, "AWAITING_PAYMENT", 0);
        String negotiating = runOrder(nimal, keells, "Beans", 150, 320, "NEGOTIATING", 0);
        chat(negotiating, keells, nimal);

        // Ratings on the completed orders
        orderService.rate(completed1, roshan.getId(), "FARMER", 5, "Grade-A onions, packed well.");
        orderService.rate(completed1, nimal.getId(), "BUYER", 5, "Paid on time.");
        orderService.rate(completed1, nimal.getId(), "TRANSPORTER", 4, "Arrived a little late.");
        orderService.rate(completed2, greenmart.getId(), "FARMER", 4, "Good quality.");
        for (String id : List.of(nimal.getId(), roshan.getId(), keells.getId(), greenmart.getId(), sunil.getId())) {
            trust.recompute(id);
        }
        log.info("SEED: users, listings, wanted ads, offer, 6 orders, ratings created");
    }

    /**
     * Buyer view: data for every buyer screen. New test users and documents only.
     * Orders at loading approval, arrived, balance paid, rejected on delivery and completed-unrated; a wanted ad with
     * two offers and a closed one; GPS tracking points on the in-transit order; a wallet balance for wallet payments.
     */
    private void seedBuyerView() {
        User nimal = users.findByEmail(FARMER_EMAIL).orElseThrow(() -> new IllegalStateException("Run the farmer seed first"));
        User roshan = users.findById("user_buyer_roshan").orElseThrow();
        User keells = users.findById("buyer_keells").orElseThrow();
        User greenmart = users.findById("buyer_greenmart").orElseThrow();
        User kamala = user(BUYER_VIEW_MARKER, "Kamala Wijesinghe", "kamala.farmer@test.harvesthub.lk", "0700000302", "FARMER",
                6.9497, 80.7891, "Kandapola, Nuwara Eliya");
        // three more drivers next to the Dambulla farm so every seeded order gets a vehicle
        driver("transporter_nuwan", "Nuwan Rathnayake", "0700000205", "LORRY", 4000, "NC LD-3021", 7.8745, 80.6515, "Dambulla");
        driver("transporter_chamara", "Chamara Dissanayake", "0700000206", "LORRY", 4000, "CP LE-8840", 7.8748, 80.6518, "Dambulla");
        driver("transporter_dilan", "Dilan Kumara", "0700000207", "TRUCK", 10000, "NC LH-1290", 7.8750, 80.6520, "Dambulla");
        driver("transporter_isuru", "Isuru Senanayake", "0700000208", "LORRY", 4000, "CP LF-4417", 7.8752, 80.6522, "Dambulla");

        evidencePhotos = listings.findAll().stream()
                .filter(l -> l.getMediaUrls() != null)
                .flatMap(l -> l.getMediaUrls().stream())
                .filter(u -> u != null && u.toLowerCase(Locale.ROOT).matches(".*\\.(jpe?g|png|webp)$"))
                .distinct().limit(3).toList();
        log.info("SEED buyer view: {} uploaded photos reused as loading evidence", evidencePhotos.size());

        // 2.4 / 3.1: an open wanted ad with two offers (two farmers) and an expired one
        WantedAd tomatoAd = wanted(roshan, "Tomatoes", "Pettah, Colombo 11", 200, 175, true);
        tomatoAd.setTargetGrade("A");
        tomatoAd.setNotes("Need 200 kg of firm tomatoes by Friday for the Pettah wholesale stall.");
        tomatoAd.setBuyerName(roshan.getFullName());
        tomatoAd.setDeadline(LocalDate.now().plusDays(3).toString());
        wantedAds.save(tomatoAd);
        Listing nimalTomatoes = listing(nimal, "Tomatoes", 260, 178, "ACTIVE", "A", null);
        Listing kamalaTomatoes = listing(kamala, "Tomatoes", 400, 160, "ACTIVE", "B", null);
        supplyOffer(tomatoAd, nimalTomatoes, nimal, 210, 175);
        supplyOffer(tomatoAd, kamalaTomatoes, kamala, 200, 160);
        listing(kamala, "Carrots", 900, 140, "ACTIVE", "A", null);
        listing(kamala, "Leeks", 1500, 120, "ACTIVE", "C", null);
        WantedAd oldAd = wanted(keells, "Big Onions", "Keells DC, Colombo 05", 500, 240, false);
        oldAd.setBuyerName(keells.getFullName());
        oldAd.setDeadline(LocalDate.now().minusDays(4).toString());
        oldAd.setStatus("CLOSED");
        oldAd.setClosedReason("DEADLINE");
        oldAd.setClosedDate(LocalDateTime.now().minusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        wantedAds.save(oldAd);

        // 5.2: GPS points on the existing in-transit order (Dambulla -> Colombo), 10 minutes apart
        orders.findByBuyerId(keells.getId()).stream().filter(o -> "IN_TRANSIT".equals(o.getStatus())).findFirst()
                .ifPresent(this::trackingPoints);

        // 5.1 / 6.1 / 6.2 / 6.3 / 6.4
        runOrder(nimal, roshan, "Pumpkin", 700, 95, "AWAITING_BUYER_LOADING_APPROVAL", 0);
        runOrder(nimal, roshan, "Green Chillies", 300, 420, "ARRIVED_AT_DESTINATION", 0);
        runOrder(kamala, greenmart, "Carrots", 450, 150, "BALANCE_PAID", 0);
        runOrder(nimal, keells, "Beetroot", 380, 165, "RETURN_JOURNEY", 2);
        runOrder(kamala, keells, "Cabbage", 520, 120, "COMPLETED", 6);

        // 4.2: wallet payments need a balance
        ledger.topUp(roshan.getId(), 150_000);
        for (String id : List.of(nimal.getId(), kamala.getId(), roshan.getId(), keells.getId(), greenmart.getId())) {
            trust.recompute(id);
        }
        log.info("SEED: buyer-view data created (1 farmer, 4 drivers, 2 wanted ads, 2 offers, 5 orders, GPS points, wallet top-up)");
    }

    /**
     * Transporter view. Existing seed already covers accepted, awaiting approval, in transit,
     * arrived, balance paid, rejected-on-delivery and completed. Adds a test farmer in Bandarawela and, next to it:
     * Tharindu (history: empty trip + completed and rated, plus a pending job request to accept live),
     * Pradeep (harvest ready → pickup map), Lahiru (at the farm → photos), Mahesh (photos approved → Start Journey).
     */
    /**
     * Desktop officer portal. Officers cannot self-register, so the second test officer is created here.
     * Listings with real photos/video, decisions and disputes are created afterwards through the live HTTPS API
     * (farmer upload + officer review + dispute endpoints), so the files exist on the server.
     */
    private void seedOfficerView() {
        User o = user(OFFICER_VIEW_MARKER, "Nadeesha Wijeratne", "officer.nadeesha@test.harvesthub.lk", "0700000401", "OFFICER",
                7.4675, 80.6234, "Matale District Agriculture Office");
        log.info("SEED officer view: test officer {} created", o.getEmail());
    }

    private void seedTransporterView() {
        users.findById(BUYER_VIEW_MARKER).orElseThrow(() -> new IllegalStateException("Run the buyer-view seed first"));
        // a new test farm in Bandarawela: no existing driver was last seen there, so dispatch picks the new drivers
        User kamala = user("farmer_sampath_test", "Sampath Herath", "sampath.farmer@test.harvesthub.lk", "0700000303", "FARMER",
                6.8328, 80.9870, "Bindunuwewa, Bandarawela");
        User roshan = users.findById("user_buyer_roshan").orElseThrow();
        User keells = users.findById("buyer_keells").orElseThrow();
        User greenmart = users.findById("buyer_greenmart").orElseThrow();
        if (evidencePhotos.isEmpty()) {
            evidencePhotos = listings.findAll().stream()
                    .filter(l -> l.getMediaUrls() != null)
                    .flatMap(l -> l.getMediaUrls().stream())
                    .filter(u -> u != null && u.toLowerCase(Locale.ROOT).matches(".*\\.(jpe?g|png|webp)$"))
                    .distinct().limit(3).toList();
        }
        double lat = kamala.getFarmLocation().getLatitude(), lng = kamala.getFarmLocation().getLongitude();

        User tharindu = driver(TRANSPORTER_VIEW_MARKER, "Tharindu Bandara", "0700000209", "LORRY", 3500, "UP LK-6120",
                lat + 0.01, lng + 0.01, "Bandarawela");
        tharindu.setVehicleBodyType("REEFER");
        tharindu.setOnline(true);
        users.save(tharindu);
        // history: an empty trip (rejected at loading) and a completed trip he already rated
        String empty = runOrder(kamala, keells, "Leeks", 600, 120, "PRELOAD_REJECTED", 0);
        backdate(empty, 3);
        String done = runOrder(kamala, greenmart, "Strawberries", 250, 900, "COMPLETED", 5);
        orderService.rate(done, tharindu.getId(), "FARMER", 5, "Fresh Harvest, On-Time Loading");
        orderService.rate(done, tharindu.getId(), "BUYER", 4, "Fast Unloading");
        // back at his base ~1 km away (the photo events left his last GPS point at the farm)
        tharindu = users.findById(tharindu.getId()).orElseThrow();
        tharindu.setLastLatitude(lat + 0.01);
        tharindu.setLastLongitude(lng + 0.01);
        users.save(tharindu);

        // one driver per remaining step; each new driver stands at the farm so dispatch picks them first
        User pradeep = driver("transporter_pradeep", "Pradeep Gunawardena", "0700000210", "LORRY", 4000, "UP LB-2245", lat, lng, "Bandarawela");
        runOrder(kamala, roshan, "Potatoes", 800, 145, "HARVEST_READY", 0);
        User lahiru = driver("transporter_lahiru", "Lahiru Madushanka", "0700000211", "MINI_LORRY", 1000, "UP QB-7781", lat, lng, "Bandarawela");
        runOrder(kamala, keells, "Cauliflower", 400, 260, "ARRIVED_AT_PICKUP", 0);
        User mahesh = driver("transporter_mahesh", "Mahesh Karunaratne", "0700000212", "TRUCK", 10000, "UP LH-9034", lat, lng, "Bandarawela");
        runOrder(kamala, greenmart, "Carrots", 1200, 140, "LOADING_APPROVED", 0);
        for (User d : List.of(pradeep, lahiru, mahesh)) {
            User fresh = users.findById(d.getId()).orElseThrow();
            fresh.setOnline(true);
            users.save(fresh);
        }

        // a job request waiting for Tharindu (no expiry, so it stays answerable for the demo)
        String request = runOrder(kamala, roshan, "Cabbage", 500, 130, "TRANSPORTER_ASSIGNED", 0);
        Order r = orders.findById(request).orElseThrow();
        if (!tharindu.getId().equals(r.getTransporterId())) {
            throw new IllegalStateException("Pending request went to " + r.getTransporterId() + " instead of Tharindu");
        }
        r.setTransportRequestExpiresAt(null);
        orders.save(r);
        for (String id : List.of(kamala.getId(), tharindu.getId(), roshan.getId(), keells.getId(), greenmart.getId())) {
            trust.recompute(id);
        }
        log.info("SEED: transporter-view data created (4 drivers, 6 orders: empty trip, completed, harvest ready, at pickup, loading approved, pending request)");
    }

    private void supplyOffer(WantedAd ad, Listing batch, User farmer, double kg, double price) {
        SupplyOffer o = new SupplyOffer();
        o.setId("offer_test_" + farmer.getId() + "_" + System.nanoTime());
        o.setRequestId(ad.getRequestId());
        o.setBatchId(batch.getId());
        o.setFarmerId(farmer.getId());
        o.setFarmerName(farmer.getFullName());
        o.setCropName(batch.getCropName());
        o.setGrade(batch.getGrade());
        o.setLocation(farmer.getFarmLocation().getAddress());
        o.setQuantity(kg);
        o.setPricePerKg(price);
        o.setStatus("PENDING");
        o.setCreatedDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        offers.save(o);
    }

    /** Three GPS points along the A9 / A1 towards the drop point, spaced 10 minutes apart (≈ 45 km/h). */
    private void trackingPoints(Order o) {
        LogisticsRecord lr = logisticsRecords.findByOrderId(o.getOrderId()).orElse(null);
        if (lr == null || lr.getPickupLatitude() == null || lr.getDropLatitude() == null) return;
        double[][] fractions = {{0.30}, {0.40}, {0.48}};
        LocalDateTime t = LocalDateTime.now().minusMinutes(20);
        for (double[] f : fractions) {
            double lat = lr.getPickupLatitude() + (lr.getDropLatitude() - lr.getPickupLatitude()) * f[0];
            double lng = lr.getPickupLongitude() + (lr.getDropLongitude() - lr.getPickupLongitude()) * f[0];
            TransporterEventRequest r = new TransporterEventRequest();
            r.setTransporterId(o.getTransporterId());
            r.setEventType("LOCATION");
            r.setLatitude(lat);
            r.setLongitude(lng);
            orderService.applyTransporterEvent(o.getOrderId(), r);
        }
        LogisticsRecord saved = logisticsRecords.findByOrderId(o.getOrderId()).orElseThrow();
        List<LogisticsRecord.TrackPoint> pts = saved.getTrackingPoints();
        for (int i = Math.max(0, pts.size() - 3), k = 0; i < pts.size(); i++, k++) {
            pts.get(i).setTimestamp(t.plusMinutes(10L * k).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        logisticsRecords.save(saved);
    }

    private Listing listing(User farmer, String crop, double kg, double price, String status, String grade, String notes) {
        Listing l = new Listing();
        l.setFarmerId(farmer.getId());
        l.setFarmerName(farmer.getFullName());
        l.setCropName(crop);
        l.setQuantity(String.valueOf((long) kg));
        l.setPrice(String.valueOf((long) price));
        l.setStatus(status);
        l.setGrade(grade);
        l.setReviewNotes(notes);
        l.setLocation(farmer.getFarmLocation().getAddress());
        l.setGpsLatitude(farmer.getFarmLocation().getLatitude());
        l.setGpsLongitude(farmer.getFarmLocation().getLongitude());
        l.setGpsCoordinates(String.format(Locale.US, "%.6f, %.6f", l.getGpsLatitude(), l.getGpsLongitude()));
        l.setReferenceCode(crop.replaceAll("[^A-Z]", "") + "-" + (2000 + Math.abs(crop.hashCode() % 900)));
        l.setVariancePercent("5");
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        l.setCreatedDate(now);
        l.setUpdatedTime(now);
        if (grade != null || "REJECTED".equals(status) || "RETAKE_REQUESTED".equals(status)) {
            l.setReviewedByOfficerName("Test Officer");
            l.setReviewedAt(now);
        }
        return listings.save(l);
    }

    private WantedAd wanted(User buyer, String crop, String location, double kg, double target, boolean urgent) {
        WantedAd ad = new WantedAd();
        ad.setRequestId("REQ-TEST-" + crop.replaceAll("\\s", "").toUpperCase() + "-" + buyer.getId().hashCode() % 1000);
        ad.setBuyerId(buyer.getId());
        ad.setCropName(crop);
        ad.setLocation(location);
        ad.setRequiredQuantityKg(kg);
        ad.setTargetPricePerKg(target);
        ad.setUrgent(urgent);
        ad.setStatus("OPEN");
        ad.setCreatedDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        ad.setDeadline(LocalDate.now().plusDays(10).toString());
        return wantedAds.save(ad);
    }

    private void chat(String orderId, User buyer, User farmer) {
        String[][] lines = {
                {buyer.getId(), "Hello Nimal, I reviewed your verified beans. Can you do LKR 310/kg for 150 kg?"},
                {farmer.getId(), "Hello. Given the certified quality, LKR 320/kg is fair. Pickup on Friday works."},
                {buyer.getId(), "OK, 320 for 150 kg. I confirmed the deal from my side."}};
        LocalDateTime t = LocalDateTime.now().minusMinutes(30);
        for (String[] l : lines) {
            ChatMessage m = new ChatMessage();
            m.setOrderId(orderId);
            m.setSenderId(l[0]);
            m.setText(l[1]);
            m.setTimestamp(t.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            t = t.plusMinutes(4);
            chats.save(m);
        }
    }

    /** Creates a listing + order and drives it to the target status. daysAgo shifts dates of finished orders. */
    private String runOrder(User farmer, User buyer, String crop, double kg, double price, String target, int daysAgo) {
        Listing l = listing(farmer, crop, kg * 1.2, price, "ACTIVE", "A", null);
        Order req = new Order();
        req.setBuyerId(buyer.getId());
        req.setBuyerName(buyer.getFullName());
        req.setListingId(l.getId());
        req.setQuantity(kg);
        req.setPricePerKg(price);
        Order o = orderService.createOrder(req);
        String id = o.getOrderId();
        if ("NEGOTIATING".equals(target)) {
            ConfirmDealRequest c = new ConfirmDealRequest();
            c.setQuantity(kg);
            c.setPricePerKg(price);
            c.setDeliveryDate(LocalDate.now().plusDays(4).toString());
            orderService.confirmDeal(id, c, buyer.getId()); // buyer confirmed; farmer still has to
            return id;
        }
        ConfirmDealRequest c = new ConfirmDealRequest();
        c.setQuantity(kg);
        c.setPricePerKg(price);
        c.setDeliveryDate(LocalDate.now().plusDays(daysAgo > 0 ? -daysAgo : 3).toString());
        orderService.confirmDeal(id, c, farmer.getId());
        orderService.confirmDeal(id, c, buyer.getId());
        if ("AWAITING_PAYMENT".equals(target)) return id;

        o = orderService.getOrderById(id);
        o = orderService.recordAdvancePayment(id, o.getPayment().getAdvanceDue(), buyer.getId());
        String driver = o.getTransporterId();
        if (driver == null) throw new IllegalStateException("No test transporter available for " + crop);
        if ("TRANSPORTER_ASSIGNED".equals(target)) return id;
        t(id, driver, "ACCEPT");
        if ("TRANSPORTER_ACCEPTED".equals(target)) return id;
        orderService.applyPartyEvent(id, "START_HARVESTING", farmer.getId(), null);
        orderService.applyPartyEvent(id, "HARVEST_READY", farmer.getId(), null);
        if ("HARVEST_READY".equals(target)) return id;
        t(id, driver, "ARRIVED_PICKUP");
        if ("ARRIVED_AT_PICKUP".equals(target)) return id;
        TransporterEventRequest ev = new TransporterEventRequest();
        ev.setTransporterId(driver);
        ev.setEventType("LOADING_EVIDENCE_SUBMITTED");
        ev.setPhotoCount(Math.max(3, evidencePhotos.size()));
        ev.setPhotoUrls(evidencePhotos);
        ev.setLatitude(farmer.getFarmLocation().getLatitude());
        ev.setLongitude(farmer.getFarmLocation().getLongitude());
        orderService.applyTransporterEvent(id, ev);
        if ("AWAITING_BUYER_LOADING_APPROVAL".equals(target)) {
            // keep it reviewable for the demo: no auto-approve timer on seeded evidence
            Order waiting = orders.findById(id).orElseThrow();
            waiting.setLoadingApprovalDeadline(null);
            orders.save(waiting);
            return id;
        }
        if ("PRELOAD_REJECTED".equals(target)) {
            orderService.applyPartyEvent(id, "REJECT_LOADING", buyer.getId(), "Leaves wilted and yellow in the photos");
            return id;
        }
        orderService.applyPartyEvent(id, "APPROVE_LOADING", buyer.getId(), null);
        if ("LOADING_APPROVED".equals(target)) return id;
        t(id, driver, "START_JOURNEY");
        if ("IN_TRANSIT".equals(target)) return id;
        t(id, driver, "ARRIVED_DESTINATION");
        if ("ARRIVED_AT_DESTINATION".equals(target)) return id;
        if ("RETURN_JOURNEY".equals(target)) {
            orderService.applyPartyEvent(id, "REJECT_DELIVERY", buyer.getId(), "Crates crushed in transit, 30% of produce bruised");
            return id;
        }
        orderService.applyPartyEvent(id, "ACCEPT_DELIVERY", buyer.getId(), null);
        if ("BALANCE_PAID".equals(target)) return id;
        t(id, driver, "DELIVERY_COMPLETED");
        orderService.applyPartyEvent(id, "GOODS_RECEIVED", buyer.getId(), null);
        if (daysAgo > 0) backdate(id, daysAgo);
        return id;
    }

    private void t(String orderId, String driver, String event) {
        TransporterEventRequest r = new TransporterEventRequest();
        r.setTransporterId(driver);
        r.setEventType(event);
        orderService.applyTransporterEvent(orderId, r);
    }

    /** Moves a finished order and its ledger entries into the past so history and monthly revenue look real. */
    private void backdate(String orderId, int daysAgo) {
        Order o = orders.findById(orderId).orElseThrow();
        String when = LocalDateTime.now().minusDays(daysAgo).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        o.setCreatedDate(LocalDateTime.now().minusDays(daysAgo + 3).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        o.setActualDeliveryDate(when);
        orders.save(o);
        String ts = LocalDateTime.now().minusDays(daysAgo).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        transactions.findAll().stream().filter(t -> orderId.equals(t.getOrderId())).forEach(t -> {
            t.setTimestamp(ts);
            transactions.save(t);
        });
        listings.findById(o.getListingId()).ifPresent(l -> {
            l.setUpdatedTime(when);
            listings.save(l);
        });
    }

    // ------------------------------------------------------------------ market prices

    record Center(String name, String province) {}

    static final List<Center> CENTERS = List.of(
            new Center("Dambulla Market", "Central Province"),
            new Center("Keppetipola Center", "Uva Province"),
            new Center("Meegoda Center", "Western Province"),
            new Center("Nuwara Eliya Center", "Central Province"),
            new Center("Thambuttegama Center", "North Central Province"));

    static final Map<String, Double> BASE = new LinkedHashMap<>();
    static {
        BASE.put("Big Onions", 240.0);
        BASE.put("Red Onions", 310.0);
        BASE.put("Tomatoes", 170.0);
        BASE.put("Potatoes", 185.0);
        BASE.put("Carrots", 215.0);
        BASE.put("Leeks", 205.0);
        BASE.put("Cabbage", 150.0);
        BASE.put("Beans", 320.0);
        BASE.put("Keeri Samba", 130.0);
    }

    /** 30 days of daily prices per crop and centre. Deterministic (seeded), labelled source = SEED_TEST. */
    private int seedMarketPrices() {
        int created = 0;
        LocalDate today = LocalDate.now();
        for (var crop : BASE.entrySet()) {
            for (Center c : CENTERS) {
                Random r = new Random((crop.getKey() + c.name()).hashCode());
                double price = crop.getValue() * (0.9 + r.nextDouble() * 0.2);
                double drift = (r.nextDouble() - 0.45) * 0.012; // most crops rise slightly
                for (int d = 29; d >= 0; d--) {
                    String date = today.minusDays(d).toString();
                    price = Math.max(40, price * (1 + drift + (r.nextDouble() - 0.5) * 0.03));
                    if (prices.existsByCropNameAndCenterAndDate(crop.getKey(), c.name(), date)) continue;
                    String supply = level(r.nextDouble() + (drift < 0 ? 0.25 : -0.1));
                    String demand = level(r.nextDouble() + (drift > 0 ? 0.25 : -0.1));
                    prices.save(new MarketPrice(null, crop.getKey(), c.name(), c.province(), date,
                            Math.round(price * 100) / 100.0, supply, demand, "SEED_TEST"));
                    created++;
                }
            }
        }
        return created;
    }

    private static String level(double x) {
        return x < 0.35 ? "LOW" : x < 0.7 ? "MEDIUM" : "HIGH";
    }
}
