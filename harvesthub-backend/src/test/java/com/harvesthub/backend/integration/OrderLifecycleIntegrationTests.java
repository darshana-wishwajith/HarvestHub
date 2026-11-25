package com.harvesthub.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harvesthub.backend.model.*;
import com.harvesthub.backend.repository.*;
import com.harvesthub.backend.service.LedgerService;
import com.harvesthub.backend.util.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end order lifecycle over HTTP with real services and a throwaway LOCAL MongoDB database
 * (mongodb://localhost:27017/harvesthub_it). Never touches Atlas. Covers workflow phases 2–5:
 * happy path with settlement, transporter decline + re-dispatch, pre-loading rejection, post-delivery rejection.
 */
@SpringBootTest(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/harvesthub_it",
        "spring.data.mongodb.database=harvesthub_it",
        "spring.data.mongodb.auto-index-creation=true",
        "google.maps.api-key=",
        "harvesthub.dispatch-retry-ms=3600000"
})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderLifecycleIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired MongoTemplate mongo;
    @Autowired JwtUtil jwt;
    @Autowired UserRepository users;
    @Autowired ListingRepository listings;
    @Autowired WalletRepository wallets;
    @Autowired NotificationRepository notifications;
    @Autowired LedgerService ledger;

    final ObjectMapper json = new ObjectMapper();
    User farmer, buyer, nearDriver, farDriver;

    @BeforeEach
    void freshDatabase() {
        assertTrue(mongo.getDb().getName().endsWith("_it"), "integration tests must use the local _it database");
        mongo.getDb().drop();
        farmer = user("it-farmer", "FARMER", 7.8742, 80.6511, "Dambulla farm");             // Dambulla
        buyer = user("it-buyer", "BUYER", 7.2906, 80.6337, "Kandy shop");                    // Kandy
        nearDriver = driver("it-driver-near", 7.87, 80.65);                                   // at Dambulla
        farDriver = driver("it-driver-far", 7.29, 80.63);                                     // at Kandy
    }

    @AfterAll
    static void cleanup(@Autowired MongoTemplate mongo) {
        mongo.getDb().drop();
    }

    // ---------------------------------------------------------------- fixtures

    User user(String id, String role, double lat, double lng, String address) {
        User u = new User();
        u.setId(id);
        u.setFullName(id);
        u.setEmail(id + "@test.harvesthub.lk");
        u.setMobileNumber("07" + Math.abs(id.hashCode() % 100_000_000));
        u.setNic("IT" + id);
        u.setPassword("x");
        u.setRole(role);
        FarmLocation loc = new FarmLocation();
        loc.setLatitude(lat);
        loc.setLongitude(lng);
        loc.setAddress(address);
        u.setFarmLocation(loc);
        return users.save(u);
    }

    User driver(String id, double lat, double lng) {
        User d = user(id, "TRANSPORTER", lat, lng, "base");
        d.setVehicleType("LORRY");
        d.setVehicleCapacityKg(4000.0);
        d.setVehiclePlate("IT-" + id);
        d.setAvailable(true);
        d.setOnline(true);
        return users.save(d);
    }

    Listing activeListing(double kg, double price) {
        Listing l = new Listing();
        l.setFarmerId(farmer.getId());
        l.setFarmerName(farmer.getFullName());
        l.setCropName("Big Onions");
        l.setQuantity(String.valueOf((long) kg));
        l.setPrice(String.valueOf((long) price));
        l.setStatus("ACTIVE");
        l.setGrade("A");
        return listings.save(l);
    }

    String token(User u) {
        return "Bearer " + jwt.generateToken(u);
    }

    ResultActions call(User as, String method, String path, Object body) throws Exception {
        var req = switch (method) {
            case "GET" -> get(path);
            case "PUT" -> put(path);
            default -> post(path);
        };
        req.header("Authorization", token(as));
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return mvc.perform(req);
    }

    JsonNode ok(User as, String method, String path, Object body) throws Exception {
        String res = call(as, method, path, body).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }

    void bad(User as, String path, Object body) throws Exception {
        call(as, "POST", path, body).andExpect(status().isBadRequest());
    }

    double balance(String userId) {
        return ledger.wallet(userId).getBalance();
    }

    JsonNode event(User as, String orderId, String event) throws Exception {
        return ok(as, "POST", "/orders/" + orderId + "/events", Map.of("event", event));
    }

    JsonNode transport(User as, String orderId, String event) throws Exception {
        return ok(as, "POST", "/orders/" + orderId + "/transporter-events", Map.of("eventType", event, "photoCount", 3));
    }

    /** Negotiation → accepted → advance paid. Returns the order id. */
    String paidOrder(double kg, double price) throws Exception {
        Listing l = activeListing(1000, price);
        String id = ok(buyer, "POST", "/orders/create", Map.of("listingId", l.getId(), "quantity", kg, "pricePerKg", price))
                .path("orderId").asText();
        String date = LocalDate.now().plusDays(3).toString();
        ok(farmer, "POST", "/orders/" + id + "/confirm-deal", Map.of("quantity", kg, "pricePerKg", price, "deliveryDate", date));
        ok(buyer, "POST", "/orders/" + id + "/confirm-deal", Map.of("quantity", kg, "pricePerKg", price, "deliveryDate", date));
        JsonNode o = ok(buyer, "GET", "/orders/" + id, null);
        assertEquals("AWAITING_PAYMENT", o.path("status").asText());
        double due = o.path("payment").path("advanceDue").asDouble();
        bad(buyer, "/orders/" + id + "/payment", Map.of("amount", due - 1));                  // must be exact
        assertEquals("TRANSPORTER_ASSIGNED", ok(buyer, "POST", "/orders/" + id + "/payment", Map.of("amount", due)).path("status").asText());
        return id;
    }

    // ---------------------------------------------------------------- scenarios

    @Test
    @org.junit.jupiter.api.Order(1)
    void happyPathSettlesExactlyAsTheWorkflowSays() throws Exception {
        String id = paidOrder(800, 250);
        JsonNode o = ok(farmer, "GET", "/orders/" + id, null);
        JsonNode p = o.path("payment");
        assertEquals("MINI_LORRY", o.path("vehicleType").asText()); // 800 kg → smallest vehicle that fits (1 ton)
        double crop = p.path("cropValue").asDouble(), transport = p.path("transportFee").asDouble();
        assertEquals(200_000, crop, 0.001);
        assertEquals(p.path("advanceCropPortion").asDouble() + transport + p.path("buyerFee").asDouble(),
                p.path("advanceDue").asDouble(), 0.01);

        // request goes to all nearby drivers (nearest first); farmer must not start before one accepts
        assertEquals(nearDriver.getId(), o.path("offeredTransporterIds").get(0).asText());
        assertEquals(farDriver.getId(), o.path("offeredTransporterIds").get(1).asText());
        bad(farmer, "/orders/" + id + "/events", Map.of("event", "START_HARVESTING"));

        // one declines → the other can still accept
        JsonNode d = transport(nearDriver, id, "DECLINE");
        assertEquals("TRANSPORTER_ASSIGNED", d.path("status").asText());
        assertEquals(1, d.path("offeredTransporterIds").size());
        JsonNode acc = transport(farDriver, id, "ACCEPT");
        assertEquals("TRANSPORTER_ACCEPTED", acc.path("status").asText());
        assertEquals(farDriver.getId(), acc.path("transporterId").asText());
        assertTrue(notifications.findByFarmerIdOrderByTimestampDesc(farmer.getId()).stream()
                .anyMatch(n -> "Vehicle Assigned - Start Harvesting".equals(n.getTitle())));

        event(farmer, id, "START_HARVESTING");
        event(farmer, id, "HARVEST_READY");
        transport(farDriver, id, "ARRIVED_PICKUP");
        transport(farDriver, id, "LOADING_EVIDENCE_SUBMITTED");
        event(buyer, id, "APPROVE_LOADING");
        assertEquals(0, balance(farmer.getId()), 0.001);
        transport(farDriver, id, "START_JOURNEY");
        assertEquals(crop * 0.20, balance(farmer.getId()), 0.01);          // 20% released at Start Journey

        transport(farDriver, id, "ARRIVED_DESTINATION");
        event(buyer, id, "ACCEPT_DELIVERY");                                // pays 80%
        assertEquals("DELIVERED", transport(farDriver, id, "DELIVERY_COMPLETED").path("status").asText());
        assertEquals("COMPLETED", event(buyer, id, "GOODS_RECEIVED").path("status").asText());

        assertEquals(crop, balance(farmer.getId()), 0.01);                  // farmer 100% of crop, 0% fee
        assertEquals(transport * 0.95, balance(farDriver.getId()), 0.01);   // driver 95%
        double buyerFee = p.path("buyerFee").asDouble();
        assertEquals(buyerFee + transport * 0.05, balance(Wallet.PLATFORM), 0.01);
        assertEquals(0, balance(Wallet.ESCROW), 0.01);                      // escrow fully paid out
        assertEquals("COMPLETED", listings.findById(o.path("listingId").asText()).orElseThrow().getStatus());

        // ratings → trust score
        ok(farmer, "POST", "/orders/" + id + "/rate", Map.of("rating", 5, "toRole", "BUYER"));
        ok(farmer, "POST", "/orders/" + id + "/rate", Map.of("rating", 4, "toRole", "TRANSPORTER"));
        bad(farmer, "/orders/" + id + "/rate", Map.of("rating", 5, "toRole", "BUYER"));   // only once
        User rated = users.findById(buyer.getId()).orElseThrow();
        assertEquals(5.0, rated.getRatingAverage());
        assertNotNull(rated.getTrustScore());
        JsonNode wallet = ok(farmer, "GET", "/wallet/" + farmer.getId() + "/summary", null);
        assertFalse(wallet.path("suspended").asBoolean());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void preLoadingRejectionRefundsBuyerAndChargesFarmerTheEmptyTripFee() throws Exception {
        String id = paidOrder(500, 200);
        JsonNode p = ok(buyer, "GET", "/orders/" + id, null).path("payment");
        transport(nearDriver, id, "ACCEPT");
        event(farmer, id, "START_HARVESTING");
        event(farmer, id, "HARVEST_READY");
        transport(nearDriver, id, "ARRIVED_PICKUP");
        transport(nearDriver, id, "LOADING_EVIDENCE_SUBMITTED");
        assertEquals("PRELOAD_REJECTED", ok(buyer, "POST", "/orders/" + id + "/events",
                Map.of("event", "REJECT_LOADING", "reason", "rotten")).path("status").asText());

        double fee = p.path("transportFee").asDouble();
        assertEquals(p.path("advanceDue").asDouble(), balance(buyer.getId()), 0.01);   // 100% incl. 3%
        assertEquals(-fee, balance(farmer.getId()), 0.01);                            // Empty Trip Fee = full transport fee
        assertEquals(fee, balance(nearDriver.getId()), 0.01);
        assertEquals(0, balance(Wallet.ESCROW), 0.01);
        assertTrue(ok(farmer, "GET", "/wallet/" + farmer.getId() + "/summary", null).path("suspended").asBoolean());

        // Suspended farmer cannot list new crops
        Listing capture = new Listing();
        capture.setCropName("Carrots");
        capture.setQuantity("100");
        capture.setPrice("150");
        capture.setGpsLatitude(7.8742);
        capture.setGpsLongitude(80.6511);
        String err = call(farmer, "POST", "/listings/create", capture).andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertTrue(err.contains("negative"), err);
        // and cannot withdraw
        bad(farmer, "/wallet/" + farmer.getId() + "/withdraw", Map.of("amount", 1));
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void postDeliveryRejectionRefundsBuyerAndChargesTheTransporter() throws Exception {
        String id = paidOrder(500, 200);
        JsonNode p = ok(buyer, "GET", "/orders/" + id, null).path("payment");
        transport(nearDriver, id, "ACCEPT");
        event(farmer, id, "START_HARVESTING");
        event(farmer, id, "HARVEST_READY");
        transport(nearDriver, id, "ARRIVED_PICKUP");
        transport(nearDriver, id, "LOADING_EVIDENCE_SUBMITTED");
        event(buyer, id, "APPROVE_LOADING");
        transport(nearDriver, id, "START_JOURNEY");
        transport(nearDriver, id, "ARRIVED_DESTINATION");
        assertEquals("RETURN_JOURNEY", ok(buyer, "POST", "/orders/" + id + "/events",
                Map.of("event", "REJECT_DELIVERY", "reason", "damaged")).path("status").asText());

        double advanceCrop = p.path("advanceCropPortion").asDouble();
        assertEquals(p.path("advanceDue").asDouble(), balance(buyer.getId()), 0.01);   // everything back
        assertEquals(advanceCrop, balance(farmer.getId()), 0.01);                     // farmer keeps the advance
        assertEquals(-advanceCrop, balance(nearDriver.getId()), 0.01);                // driver pays for the crops, earns 0
        assertEquals(0, balance(Wallet.ESCROW), 0.01);
        // The driver takes the goods back to the farm first, then is free again
        assertEquals(Boolean.FALSE, users.findById(nearDriver.getId()).orElseThrow().getAvailable());
        assertEquals("RETURNED", transport(nearDriver, id, "GOODS_RETURNED").path("status").asText());
        assertEquals(Boolean.TRUE, users.findById(nearDriver.getId()).orElseThrow().getAvailable());
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    void noDriverMeansPausedAndOnlyPartiesCanSeeTheOrder() throws Exception {
        nearDriver.setOnline(false);
        users.save(nearDriver);
        farDriver.setOnline(false);
        users.save(farDriver);
        Listing l = activeListing(1000, 100);
        String id = ok(buyer, "POST", "/orders/create", Map.of("listingId", l.getId(), "quantity", 300, "pricePerKg", 100))
                .path("orderId").asText();
        String date = LocalDate.now().plusDays(2).toString();
        ok(farmer, "POST", "/orders/" + id + "/confirm-deal", Map.of("quantity", 300, "pricePerKg", 100, "deliveryDate", date));
        ok(buyer, "POST", "/orders/" + id + "/confirm-deal", Map.of("quantity", 300, "pricePerKg", 100, "deliveryDate", date));
        double due = ok(buyer, "GET", "/orders/" + id, null).path("payment").path("advanceDue").asDouble();
        assertEquals("PAUSED", ok(buyer, "POST", "/orders/" + id + "/payment", Map.of("amount", due)).path("status").asText());

        // a stranger (another farmer) cannot read it
        User stranger = user("it-stranger", "FARMER", 7.0, 80.0, "x");
        call(stranger, "GET", "/orders/" + id, null).andExpect(status().isForbidden());
        // farmer cannot set arbitrary statuses
        call(farmer, "PUT", "/orders/" + id + "/status", Map.of("status", "COMPLETED")).andExpect(status().isForbidden());
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    void listingIsLocationLockedAndNeedsTheCapturedMedia() throws Exception {
        // upload 3 files (JPEG magic bytes are enough for the type check)
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1, 1, 0};
        java.util.List<String> urls = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String res = mvc.perform(multipart("/media/upload")
                            .file(new org.springframework.mock.web.MockMultipartFile("file", "c.jpg", "image/jpeg", jpeg))
                            .header("Authorization", token(farmer)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            urls.add(json.readTree(res).path("url").asText());
        }
        Map<String, Object> capture = new java.util.HashMap<>(Map.of(
                "cropName", "Dragon Fruit", "quantity", "120", "price", "600", "mediaUrls", urls));

        // 2 km away from the registered farm → refused
        capture.put("gpsLatitude", 7.8920);
        capture.put("gpsLongitude", 80.6511);
        String err = call(farmer, "POST", "/listings/create", capture).andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertTrue(err.contains("registered farm"), err);
        assertTrue(ok(farmer, "GET", "/listings/location-check?lat=7.8920&lng=80.6511", null).path("withinFarm").asBoolean() == false);

        // missing video → refused
        capture.put("gpsLatitude", 7.8743);
        capture.put("gpsLongitude", 80.6512);
        capture.put("mediaUrls", urls.subList(0, 2));
        bad(farmer, "/listings/create", capture);

        // at the farm with all media → Pending Verification; unknown crop added as pending
        capture.put("mediaUrls", urls);
        JsonNode l = ok(farmer, "POST", "/listings/create", capture);
        assertEquals("PENDING", l.path("status").asText());
        assertTrue(l.path("cropPending").asBoolean());
        assertEquals(3, l.path("mediaUrls").size());
        assertTrue(ok(farmer, "GET", "/api/crops", null).toString().contains("Dragon Fruit"));
        // buyers cannot create listings
        call(buyer, "POST", "/listings/create", capture).andExpect(status().isForbidden());
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    void photoThumbnailsAreSmallJpegsMadeOnce() throws Exception {
        java.awt.image.BufferedImage big = new java.awt.image.BufferedImage(1600, 1200, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = big.createGraphics();
        g.setColor(java.awt.Color.GREEN);
        g.fillRect(0, 0, 1600, 1200);
        g.dispose();
        java.io.ByteArrayOutputStream jpg = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(big, "jpg", jpg);
        String url = json.readTree(mvc.perform(multipart("/media/upload")
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "p.jpg", "image/jpeg", jpg.toByteArray()))
                        .header("Authorization", token(farmer)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("url").asText();
        String name = url.substring("/uploads/".length());

        // public (no token), JPEG, longest side = the size bucket (200 -> 320)
        byte[] thumb = mvc.perform(get("/uploads/thumb/" + name + "?size=200")).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/jpeg"))
                .andReturn().getResponse().getContentAsByteArray();
        java.awt.image.BufferedImage t = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(thumb));
        assertEquals(320, t.getWidth());
        assertEquals(240, t.getHeight());
        assertTrue(thumb.length < jpg.size());
        // second request is served from the saved copy (same bytes)
        assertArrayEquals(thumb, mvc.perform(get("/uploads/thumb/" + name + "?size=320")).andReturn().getResponse().getContentAsByteArray());
        // not for videos or made-up names
        mvc.perform(get("/uploads/thumb/00000000-0000-0000-0000-000000000000.mp4")).andExpect(status().isNotFound());
        mvc.perform(get("/uploads/thumb/..%2Fsecret.jpg")).andExpect(status().is4xxClientError());
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    void farmerSeesAnOpenNegotiationOnTheListing() throws Exception {
        Listing l = activeListing(500, 250);
        String orderId = ok(buyer, "POST", "/orders/create", Map.of("listingId", l.getId(), "quantity", 100, "pricePerKg", 240))
                .path("orderId").asText();
        JsonNode mine = ok(farmer, "GET", "/farmer/" + farmer.getId() + "/active-listings", null);
        JsonNode card = null;
        for (JsonNode n : mine) if (l.getId().equals(n.path("id").asText())) card = n;
        assertNotNull(card);
        assertEquals("ACTIVE", card.path("status").asText());              // still on the market while negotiating
        assertEquals(orderId, card.path("negotiationOrderId").asText());   // ...but the farmer can open the chat
        assertEquals(1, card.path("negotiationCount").asInt());
        // chat timestamps have whole seconds
        String ts = ok(farmer, "POST", "/chat/" + orderId + "/send", Map.of("text", "Hello")).path("timestamp").asText();
        assertTrue(ts.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}"), ts);
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    void farmerCardActionsEditDeactivateReactivate() throws Exception {
        Listing l = activeListing(500, 250);                       // Listed, grade A
        // Edit = yield + price only; Listed keeps its status and grade
        JsonNode e = ok(farmer, "PUT", "/listings/" + l.getId(), Map.of("quantity", "450", "price", "260", "cropName", "Leeks"));
        assertEquals("450", e.path("quantity").asText());
        assertEquals("260", e.path("price").asText());
        assertEquals("Big Onions", e.path("cropName").asText());   // crop is not editable
        assertEquals("ACTIVE", e.path("status").asText());
        assertEquals("A", e.path("grade").asText());
        call(farmer, "PUT", "/listings/" + l.getId(), Map.of("price", "0")).andExpect(status().isBadRequest());
        call(buyer, "PUT", "/listings/" + l.getId(), Map.of("price", "300")).andExpect(status().isForbidden());

        // Deactivate: hidden from the marketplace, still in the farmer's list
        assertEquals("DEACTIVATED", ok(farmer, "POST", "/listings/" + l.getId() + "/deactivate", null).path("status").asText());
        assertFalse(ok(buyer, "GET", "/buyer/listings", null).toString().contains(l.getId()));
        assertTrue(ok(farmer, "GET", "/farmer/" + farmer.getId() + "/active-listings", null).toString().contains("DEACTIVATED"));
        bad(farmer, "/listings/" + l.getId() + "/deactivate", null);   // already deactivated

        // Re-activate: a Listed lot comes back Listed with the same grade
        JsonNode r = ok(farmer, "POST", "/listings/" + l.getId() + "/reactivate", null);
        assertEquals("ACTIVE", r.path("status").asText());
        assertEquals("A", r.path("grade").asText());

        // A pending lot goes back to the officer queue
        Listing p = activeListing(100, 100);
        p.setStatus("PENDING");
        p.setGrade(null);
        listings.save(p);
        ok(farmer, "POST", "/listings/" + p.getId() + "/deactivate", null);
        assertEquals("PENDING", ok(farmer, "POST", "/listings/" + p.getId() + "/reactivate", null).path("status").asText());

        // Not someone else's listing, and not while an order is running
        User other = user("it-farmer-2", "FARMER", 7.87, 80.65, "other");
        bad(other, "/listings/" + p.getId() + "/deactivate", null);
        p.setActiveOrderId("ORD-1");
        listings.save(p);
        bad(farmer, "/listings/" + p.getId() + "/deactivate", null);
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    void jsonFieldNamesMatchTheAndroidModels() throws Exception {
        WantedAd ad = new WantedAd();
        ad.setRequestId("REQ-IT");
        ad.setBuyerId(buyer.getId());
        ad.setCropName("Big Onions");
        ad.setRequiredQuantityKg(100);
        ad.setTargetPricePerKg(200);
        ad.setUrgent(true);
        ad.setStatus("OPEN");
        mongo.save(ad);
        assertTrue(ok(farmer, "GET", "/wanted-ads", null).get(0).path("isUrgent").asBoolean());

        ledger.transfer(null, farmer.getId(), 100, "TEST", "x", "credit test", null);
        assertTrue(ok(farmer, "GET", "/wallet/" + farmer.getId() + "/transactions", null).get(0).path("isCredit").asBoolean());

        mongo.save(new NotificationItem(null, farmer.getId(), "TEST", "t", "b", "2026-01-01T00:00:00", true));
        assertTrue(ok(farmer, "GET", "/notifications/" + farmer.getId(), null).get(0).has("isRead"));
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    void counterOfferResetsTheOtherPartysConfirmation() throws Exception {
        Listing l = activeListing(1000, 250);
        String id = ok(buyer, "POST", "/orders/create", Map.of("listingId", l.getId(), "quantity", 500, "pricePerKg", 240))
                .path("orderId").asText();
        String date = LocalDate.now().plusDays(3).toString();
        ok(buyer, "POST", "/orders/" + id + "/confirm-deal", Map.of("quantity", 500, "pricePerKg", 240, "deliveryDate", date));
        // farmer counters with a higher price → buyer must confirm again
        ok(farmer, "POST", "/orders/" + id + "/confirm-deal", Map.of("quantity", 500, "pricePerKg", 250, "deliveryDate", date));
        JsonNode o = ok(buyer, "GET", "/orders/" + id, null);
        assertEquals("NEGOTIATING", o.path("status").asText());
        assertTrue(o.path("farmerConfirmed").asBoolean());
        assertFalse(o.path("buyerConfirmed").asBoolean());
        ok(buyer, "POST", "/orders/" + id + "/confirm-deal", Map.of("quantity", 500, "pricePerKg", 250, "deliveryDate", date));
        assertEquals("AWAITING_PAYMENT", ok(buyer, "GET", "/orders/" + id, null).path("status").asText());
        assertEquals("ONGOING", listings.findById(l.getId()).orElseThrow().getStatus());
        // too much quantity is refused
        bad(buyer, "/orders/create", Map.of("listingId", l.getId(), "quantity", 5000, "pricePerKg", 250));
    }
}
