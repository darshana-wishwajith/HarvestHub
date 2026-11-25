package com.harvesthub.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harvesthub.backend.model.*;
import com.harvesthub.backend.repository.*;
import com.harvesthub.backend.service.LedgerService;
import com.harvesthub.backend.service.OrderService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Buyer view over HTTP with real services on the LOCAL
 * mongodb://localhost:27017/harvesthub_it database. Never touches Atlas. Transporter steps are driven through the API.
 */
@SpringBootTest(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/harvesthub_it",
        "spring.data.mongodb.database=harvesthub_it",
        "spring.data.mongodb.auto-index-creation=true",
        "google.maps.api-key=",
        "harvesthub.dispatch-retry-ms=3600000",
        "harvesthub.loading-check-ms=3600000",
        "harvesthub.wanted-ad-check-ms=3600000"
})
@AutoConfigureMockMvc
class BuyerFlowIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired MongoTemplate mongo;
    @Autowired JwtUtil jwt;
    @Autowired UserRepository users;
    @Autowired ListingRepository listings;
    @Autowired OrderRepository orders;
    @Autowired WantedAdRepository wantedAds;
    @Autowired SupplyOfferRepository offers;
    @Autowired NotificationRepository notifications;
    @Autowired LedgerService ledger;
    @Autowired OrderService orderService;

    final ObjectMapper json = new ObjectMapper();
    User farmer, farmer2, buyer, driver, officer;

    @BeforeEach
    void freshDatabase() {
        assertTrue(mongo.getDb().getName().endsWith("_it"), "integration tests must use the local _it database");
        mongo.getDb().drop();
        farmer = user("bf-farmer", "FARMER", 7.8742, 80.6511, "Dambulla farm");
        farmer2 = user("bf-farmer2", "FARMER", 6.9497, 80.7891, "Nuwara Eliya farm");
        buyer = user("bf-buyer", "BUYER", 6.9271, 79.8612, "Colombo depot");
        officer = user("bf-officer", "OFFICER", 7.0, 80.0, "office");
        driver = user("bf-driver", "TRANSPORTER", 7.87, 80.65, "base");
        driver.setVehicleType("LORRY");
        driver.setVehicleCapacityKg(4000.0);
        driver.setVehiclePlate("WP-L0492");
        driver.setAvailable(true);
        driver.setOnline(true);
        driver = users.save(driver);
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

    Listing listing(User owner, String crop, String grade, double kg, double price, String location) {
        Listing l = new Listing();
        l.setFarmerId(owner.getId());
        l.setFarmerName(owner.getFullName());
        l.setCropName(crop);
        l.setQuantity(String.valueOf((long) kg));
        l.setPrice(String.valueOf((long) price));
        l.setStatus("ACTIVE");
        l.setGrade(grade);
        l.setLocation(location);
        return listings.save(l);
    }

    ResultActions call(User as, String method, String path, Object body) throws Exception {
        var req = switch (method) {
            case "GET" -> get(path);
            case "PUT" -> put(path);
            default -> post(path);
        };
        req.header("Authorization", "Bearer " + jwt.generateToken(as));
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return mvc.perform(req);
    }

    JsonNode ok(User as, String method, String path, Object body) throws Exception {
        return json.readTree(call(as, method, path, body).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    String bad(User as, String method, String path, Object body) throws Exception {
        return call(as, method, path, body).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
    }

    JsonNode transport(String orderId, Map<String, Object> body) throws Exception {
        return ok(driver, "POST", "/orders/" + orderId + "/transporter-events", body);
    }

    JsonNode transport(String orderId, String event) throws Exception {
        return transport(orderId, Map.of("eventType", event, "photoCount", 2));
    }

    JsonNode event(String orderId, String event, String method) throws Exception {
        Map<String, String> body = new HashMap<>(Map.of("event", event));
        if (method != null) body.put("method", method);
        return ok(buyer, "POST", "/orders/" + orderId + "/events", body);
    }

    /** Purchase request → both confirm → AWAITING_PAYMENT. Returns the order id. */
    String acceptedOrder(double kg, double price) throws Exception {
        Listing l = listing(farmer, "Tomatoes", "A", 1000, price, "Dambulla");
        String id = ok(buyer, "POST", "/orders/create", Map.of("listingId", l.getId(), "quantity", kg, "pricePerKg", price))
                .path("orderId").asText();
        String date = LocalDate.now().plusDays(2).toString();
        ok(farmer, "POST", "/orders/" + id + "/confirm-deal", Map.of("quantity", kg, "pricePerKg", price, "deliveryDate", date));
        ok(buyer, "POST", "/orders/" + id + "/confirm-deal", Map.of("quantity", kg, "pricePerKg", price, "deliveryDate", date));
        return id;
    }

    double advanceDue(String id) throws Exception {
        return ok(buyer, "GET", "/orders/" + id, null).path("payment").path("advanceDue").asDouble();
    }

    /** Paid by card, driver accepted, farmer harvested, driver at the farm. */
    String orderAtPickup() throws Exception {
        String id = acceptedOrder(400, 175);
        ok(buyer, "POST", "/orders/" + id + "/payment", Map.of("amount", advanceDue(id), "method", "CARD"));
        transport(id, "ACCEPT");
        ok(farmer, "POST", "/orders/" + id + "/events", Map.of("event", "START_HARVESTING"));
        ok(farmer, "POST", "/orders/" + id + "/events", Map.of("event", "HARVEST_READY"));
        transport(id, "ARRIVED_PICKUP");
        return id;
    }

    // ---------------------------------------------------------------- 4.2 payment: card or wallet

    @Test
    void walletPaymentNeedsBalanceAndShowsAsEscrowHeld() throws Exception {
        String id = acceptedOrder(400, 175);
        double due = advanceDue(id);

        String err = bad(buyer, "POST", "/orders/" + id + "/payment", Map.of("amount", due, "method", "WALLET"));
        assertTrue(err.contains("Insufficient wallet balance"), err);
        bad(buyer, "POST", "/wallet/" + buyer.getId() + "/deposit", Map.of("amount", -5));
        call(farmer, "POST", "/wallet/" + buyer.getId() + "/deposit", Map.of("amount", 100)).andExpect(status().isForbidden());

        ok(buyer, "POST", "/wallet/" + buyer.getId() + "/deposit", Map.of("amount", due + 1000));
        JsonNode paid = ok(buyer, "POST", "/orders/" + id + "/payment", Map.of("amount", due, "method", "WALLET"));
        assertEquals("TRANSPORTER_ASSIGNED", paid.path("status").asText());
        assertEquals("WALLET", paid.path("advancePaymentMethod").asText());
        assertEquals(1000, ledger.wallet(buyer.getId()).getBalance(), 0.01);
        assertEquals(due, ledger.wallet(Wallet.ESCROW).getBalance(), 0.01);

        JsonNode w = ok(buyer, "GET", "/wallet/" + buyer.getId() + "/summary", null);
        assertEquals(due, w.path("escrowHeld").asDouble(), 0.01);
        assertEquals(due + 1000, w.path("todayInflow").asDouble(), 0.01);

        JsonNode d = ok(buyer, "GET", "/dashboard/buyer/" + buyer.getId(), null);
        assertEquals(1, d.path("activeOrders").asInt());
        assertEquals(due, d.path("escrowHeld").asDouble(), 0.01);
        assertEquals(id, d.path("escrowOrders").get(0).path("orderId").asText());
        assertTrue(d.path("hasDeliveryLocation").asBoolean());
        call(farmer, "GET", "/dashboard/buyer/" + buyer.getId(), null).andExpect(status().isForbidden());
    }

    @Test
    void cardPaymentDoesNotTouchTheBuyerWallet() throws Exception {
        String id = acceptedOrder(400, 175);
        ok(buyer, "POST", "/orders/" + id + "/payment", Map.of("amount", advanceDue(id)));
        assertEquals("CARD", orders.findById(id).orElseThrow().getAdvancePaymentMethod());
        assertEquals(0, ledger.wallet(buyer.getId()).getBalance(), 0.01);
        bad(buyer, "POST", "/orders/" + id + "/payment", Map.of("amount", 1, "method", "CHEQUE"));
    }

    // ---------------------------------------------------------------- 5.1 evidence + 15 min auto-approve

    @Test
    void loadingEvidenceCarriesPhotosAndIsAutoApprovedAfterTheDeadline() throws Exception {
        String id = orderAtPickup();
        long before = System.currentTimeMillis();
        JsonNode o = transport(id, Map.of("eventType", "LOADING_EVIDENCE_SUBMITTED",
                "photoUrls", List.of("/uploads/a.jpg", "/uploads/b.jpg"), "latitude", 7.8742, "longitude", 80.6511));
        assertEquals("AWAITING_BUYER_LOADING_APPROVAL", o.path("status").asText());
        assertEquals(2, o.path("loadingEvidenceUrls").size());
        long deadline = o.path("loadingApprovalDeadline").asLong();
        assertTrue(deadline >= before + 14 * 60_000L && deadline <= System.currentTimeMillis() + 15 * 60_000L);

        orderService.autoApproveExpiredLoading();                       // not yet expired
        assertEquals("AWAITING_BUYER_LOADING_APPROVAL", orders.findById(id).orElseThrow().getStatus());

        com.harvesthub.backend.model.Order stored = orders.findById(id).orElseThrow();
        stored.setLoadingApprovalDeadline(System.currentTimeMillis() - 1000);
        orders.save(stored);
        orderService.autoApproveExpiredLoading();
        com.harvesthub.backend.model.Order approved = orders.findById(id).orElseThrow();
        assertEquals("LOADING_APPROVED", approved.getStatus());
        assertTrue(approved.isLoadingAutoApproved());
        assertTrue(notifications.findByFarmerIdOrderByTimestampDesc(buyer.getId()).stream()
                .anyMatch(n -> "LOADING_AUTO_APPROVED".equals(n.getType())));
        // buyer can no longer reject
        bad(buyer, "POST", "/orders/" + id + "/events", Map.of("event", "REJECT_LOADING"));
    }

    // ---------------------------------------------------------------- 5.2 tracking, 6.1/6.2 accept + handshake, dispute

    @Test
    void trackingBalanceByWalletHandshakeAndDispute() throws Exception {
        String id = orderAtPickup();
        transport(id, "LOADING_EVIDENCE_SUBMITTED");
        event(id, "APPROVE_LOADING", null);
        transport(id, "START_JOURNEY");

        JsonNode t0 = ok(buyer, "GET", "/orders/" + id + "/tracking", null);
        assertEquals("IN_TRANSIT", t0.path("status").asText());
        double total = t0.path("totalKm").asDouble();
        assertTrue(total > 0);
        assertTrue(t0.path("etaMinutes").asInt() > 0);

        // two GPS pings on the way to Colombo
        transport(id, Map.of("eventType", "LOCATION", "latitude", 7.40, "longitude", 80.40));
        transport(id, Map.of("eventType", "LOCATION", "latitude", 7.20, "longitude", 80.20));
        JsonNode t = ok(buyer, "GET", "/orders/" + id + "/tracking", null);
        assertEquals(7.20, t.path("currentLatitude").asDouble(), 1e-6);
        assertTrue(t.path("remainingKm").asDouble() < total);
        assertTrue(t.path("progressPercent").asInt() > 5);
        assertEquals("WP-L0492", t.path("vehiclePlate").asText());
        User stranger = user("bf-stranger", "BUYER", 7.0, 80.0, "x");
        call(stranger, "GET", "/orders/" + id + "/tracking", null).andExpect(status().isForbidden());

        transport(id, "ARRIVED_DESTINATION");
        assertEquals(100, ok(buyer, "GET", "/orders/" + id + "/tracking", null).path("progressPercent").asInt());

        // dispute only after acceptance
        bad(buyer, "POST", "/orders/" + id + "/disputes", Map.of("reason", "Short weight"));

        double balanceDue = orders.findById(id).orElseThrow().getPayment().getBalanceDue();
        String err = bad(buyer, "POST", "/orders/" + id + "/events", Map.of("event", "ACCEPT_DELIVERY", "method", "WALLET"));
        assertTrue(err.contains("Insufficient"), err);
        ok(buyer, "POST", "/wallet/" + buyer.getId() + "/deposit", Map.of("amount", balanceDue));
        JsonNode acc = event(id, "ACCEPT_DELIVERY", "WALLET");
        assertEquals("BALANCE_PAID", acc.path("status").asText());
        assertEquals("WALLET", acc.path("balancePaymentMethod").asText());
        assertEquals(0, ledger.wallet(buyer.getId()).getBalance(), 0.01);

        transport(id, "DELIVERY_COMPLETED");
        assertEquals("COMPLETED", event(id, "GOODS_RECEIVED", null).path("status").asText());
        assertEquals(0, ledger.wallet(Wallet.ESCROW).getBalance(), 0.01);

        JsonNode d = ok(buyer, "POST", "/orders/" + id + "/disputes", Map.of("reason", "Short weight", "details", "20 kg missing"));
        assertEquals("OPEN", d.path("status").asText());
        assertEquals("BUYER", d.path("raisedByRole").asText());
        assertTrue(orders.findById(id).orElseThrow().isDisputeOpen());
        assertEquals(1, ok(buyer, "GET", "/orders/" + id + "/disputes", null).size());
        assertTrue(notifications.findByFarmerIdOrderByTimestampDesc(officer.getId()).stream()
                .anyMatch(n -> "DISPUTE_OPENED".equals(n.getType())));
        bad(buyer, "POST", "/orders/" + id + "/disputes", Map.of("reason", "again"));      // one open ticket per user
        assertEquals(0, ledger.wallet(Wallet.ESCROW).getBalance(), 0.01);                    // no money moved

        // Buyer rates farmer and transporter
        ok(buyer, "POST", "/orders/" + id + "/rate", Map.of("rating", 4, "toRole", "FARMER"));
        ok(buyer, "POST", "/orders/" + id + "/rate", Map.of("rating", 5, "toRole", "TRANSPORTER"));
        assertEquals(5.0, users.findById(driver.getId()).orElseThrow().getRatingAverage());
        assertEquals(1, ok(buyer, "GET", "/wallet/" + buyer.getId() + "/summary", null).path("escrowReleased").asDouble() > 0 ? 1 : 0);
    }

    // ---------------------------------------------------------------- 2.3/2.4/3.1 wanted ads + offers

    @Test
    void wantedAdLifecycle() throws Exception {
        String deadline = LocalDate.now().plusDays(4).toString();
        JsonNode ad = ok(buyer, "POST", "/wanted-ads/create", Map.of("cropName", "Tomatoes", "requiredQuantityKg", 200,
                "targetPricePerKg", 170, "deadline", deadline, "targetGrade", "A", "notes", "Need by Friday", "location", "Colombo"));
        String adId = ad.path("requestId").asText();
        assertEquals("bf-buyer", ad.path("buyerName").asText());
        assertEquals("Need by Friday", ad.path("notes").asText());

        Listing b1 = listing(farmer, "Tomatoes", "A", 300, 180, "Dambulla");
        Listing b2 = listing(farmer2, "Tomatoes", "B", 300, 160, "Nuwara Eliya");
        ok(farmer, "POST", "/offers/create", Map.of("requestId", adId, "batchId", b1.getId(), "quantity", 210, "pricePerKg", 175));
        ok(farmer2, "POST", "/offers/create", Map.of("requestId", adId, "batchId", b2.getId(), "quantity", 200, "pricePerKg", 160));

        JsonNode mine = ok(buyer, "GET", "/wanted-ads/buyer/" + buyer.getId(), null);
        assertEquals(2, mine.get(0).path("offerCount").asInt());
        assertEquals(2, mine.get(0).path("pendingOfferCount").asInt());
        assertEquals(2, ok(buyer, "GET", "/dashboard/buyer/" + buyer.getId(), null).path("pendingOffers").asInt());

        // edit while open; another buyer cannot
        assertEquals(250, ok(buyer, "PUT", "/wanted-ads/" + adId, Map.of("requiredQuantityKg", 250, "targetPricePerKg", 170))
                .path("requiredQuantityKg").asDouble(), 0.01);
        User other = user("bf-buyer2", "BUYER", 6.9, 79.9, "y");
        call(other, "PUT", "/wanted-ads/" + adId, Map.of("requiredQuantityKg", 1)).andExpect(status().isForbidden());

        // accept farmer 1 → order NEGOTIATING, ad closed, farmer 2's offer declined
        String offer1 = offers.findByRequestId(adId).stream().filter(o -> o.getFarmerId().equals(farmer.getId())).findFirst().orElseThrow().getId();
        JsonNode order = ok(buyer, "POST", "/offers/" + offer1 + "/accept", null);
        assertEquals("NEGOTIATING", order.path("status").asText());
        assertEquals("SUPPLY_OFFER", order.path("origin").asText());
        WantedAd closed = wantedAds.findById(adId).orElseThrow();
        assertEquals("CLOSED", closed.getStatus());
        assertEquals("OFFER_ACCEPTED", closed.getClosedReason());
        assertTrue(offers.findByRequestId(adId).stream().anyMatch(o -> "DECLINED".equals(o.getStatus())));
        bad(buyer, "PUT", "/wanted-ads/" + adId, Map.of("requiredQuantityKg", 300));
        bad(farmer2, "POST", "/offers/create", Map.of("requestId", adId, "batchId", b2.getId(), "quantity", 200, "pricePerKg", 150));
        assertFalse(ok(farmer, "GET", "/wanted-ads", null).toString().contains(adId));       // gone from the farmer feed

        // republish → new open copy with the new deadline
        bad(buyer, "POST", "/wanted-ads/" + adId + "/republish", Map.of("deadline", LocalDate.now().minusDays(1).toString()));
        JsonNode copy = ok(buyer, "POST", "/wanted-ads/" + adId + "/republish", Map.of("deadline", LocalDate.now().plusDays(7).toString()));
        assertEquals("OPEN", copy.path("status").asText());
        assertEquals(adId, copy.path("republishedFrom").asText());
        assertNotEquals(adId, copy.path("requestId").asText());

        // manual close
        assertEquals("CLOSED", ok(buyer, "POST", "/wanted-ads/" + copy.path("requestId").asText() + "/close", null).path("status").asText());

        // deadline passed → closed automatically when listed
        WantedAd old = new WantedAd();
        old.setRequestId("REQ-OLD");
        old.setBuyerId(buyer.getId());
        old.setCropName("Carrots");
        old.setRequiredQuantityKg(50);
        old.setStatus("OPEN");
        old.setDeadline(LocalDate.now().minusDays(2).toString());
        old.setCreatedDate("2026-01-01T00:00:00");
        wantedAds.save(old);
        ok(buyer, "GET", "/wanted-ads/buyer/" + buyer.getId(), null);
        assertEquals("DEADLINE", wantedAds.findById("REQ-OLD").orElseThrow().getClosedReason());
    }

    // ---------------------------------------------------------------- 2.1/2.2 marketplace filters, 1.2 location

    @Test
    void marketplaceFiltersAndDeliveryLocation() throws Exception {
        listing(farmer, "Big Onions", "A", 500, 180, "Dambulla");
        listing(farmer2, "Red Potatoes", "B", 800, 140, "Nuwara Eliya");
        listing(farmer, "Green Chillies", "C", 1500, 95, "Jaffna");

        assertEquals(3, ok(buyer, "GET", "/buyer/listings", null).size());
        assertEquals(2, ok(buyer, "GET", "/buyer/listings?grade=A,B", null).size());
        assertEquals(1, ok(buyer, "GET", "/buyer/listings?district=nuwara", null).size());
        assertEquals(2, ok(buyer, "GET", "/buyer/listings?maxPrice=150", null).size());
        assertEquals(2, ok(buyer, "GET", "/buyer/listings?minQty=800", null).size());
        assertEquals("Green Chillies", ok(buyer, "GET", "/buyer/listings?maxPrice=150&minQty=1000", null).get(0).path("cropName").asText());

        JsonNode loc = ok(buyer, "PUT", "/api/me/delivery-location", Map.of("latitude", 6.91, "longitude", 79.88, "address", "Pettah"));
        assertEquals("Pettah", loc.path("address").asText());
        assertEquals("Pettah", users.findById(buyer.getId()).orElseThrow().getFarmLocation().getAddress());
        bad(buyer, "PUT", "/api/me/delivery-location", Map.of("latitude", 51.5, "longitude", -0.12));
        call(farmer, "PUT", "/api/me/delivery-location", Map.of("latitude", 6.91, "longitude", 79.88)).andExpect(status().isForbidden());
    }

    @Test
    void buyerRegistrationNeedsADeliveryPin() throws Exception {
        Map<String, Object> req = new HashMap<>(Map.of("fullName", "New Buyer", "mobileNumber", "0771234567",
                "nic", "200012345678", "email", "new.buyer@test.harvesthub.lk", "password", "Str0ng!Pass", "role", "BUYER"));
        String err = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertTrue(err.contains("Delivery location"), err);
        req.put("farmLocation", Map.of("address", "Colombo 11", "latitude", 6.94, "longitude", 79.85));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().is2xxSuccessful());
        User created = users.findByEmail("new.buyer@test.harvesthub.lk").orElseThrow();
        assertEquals("BUYER", created.getRole());
        assertEquals("Colombo 11", created.getFarmLocation().getAddress());
    }
}
