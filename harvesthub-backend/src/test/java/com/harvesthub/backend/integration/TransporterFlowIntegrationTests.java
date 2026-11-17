package com.harvesthub.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harvesthub.backend.model.*;
import com.harvesthub.backend.repository.*;
import com.harvesthub.backend.service.DispatchService;
import com.harvesthub.backend.service.LedgerService;
import com.harvesthub.backend.service.OrderService;
import com.harvesthub.backend.util.JwtUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * Transporter view (Figma page 612:58, decisions 15 Nov) over HTTP with real services on the LOCAL
 * mongodb://localhost:27017/harvesthub_it database. Never touches Atlas.
 */
@SpringBootTest(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/harvesthub_it",
        "spring.data.mongodb.database=harvesthub_it",
        "spring.data.mongodb.auto-index-creation=true",
        "google.maps.api-key=",
        "harvesthub.dispatch-retry-ms=3600000",
        "harvesthub.loading-check-ms=3600000",
        "harvesthub.wanted-ad-check-ms=3600000",
        "harvesthub.request-check-ms=3600000"
})
@AutoConfigureMockMvc
class TransporterFlowIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired MongoTemplate mongo;
    @Autowired JwtUtil jwt;
    @Autowired UserRepository users;
    @Autowired ListingRepository listings;
    @Autowired OrderRepository orders;
    @Autowired LedgerService ledger;
    @Autowired OrderService orderService;
    @Autowired DispatchService dispatchService;

    final ObjectMapper json = new ObjectMapper();
    User farmer, buyer, driver, driver2;

    @BeforeEach
    void freshDatabase() {
        assertTrue(mongo.getDb().getName().endsWith("_it"), "integration tests must use the local _it database");
        mongo.getDb().drop();
        farmer = user("tf-farmer", "FARMER", 7.8742, 80.6511, "Dambulla farm");
        buyer = user("tf-buyer", "BUYER", 6.9271, 79.8612, "Colombo depot");
        driver = driverAt("tf-driver", 7.87, 80.65);      // nearest to the farm
        driver2 = driverAt("tf-driver2", 7.29, 80.63);    // Kandy
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

    User driverAt(String id, double lat, double lng) {
        User d = user(id, "TRANSPORTER", lat, lng, "base");
        d.setVehicleType("LORRY");
        d.setVehiclePlate("WP LB-" + (1000 + Math.abs(id.hashCode() % 9000)));
        d.setAvailable(true);
        return users.save(d);
    }

    ResultActions call(User as, String method, String path, Object body) throws Exception {
        var req = switch (method) {
            case "GET" -> get(path);
            case "PUT" -> put(path);
            default -> post(path);
        };
        if (as != null) req.header("Authorization", "Bearer " + jwt.generateToken(as));
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return mvc.perform(req);
    }

    JsonNode ok(User as, String method, String path, Object body) throws Exception {
        return json.readTree(call(as, method, path, body).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    String bad(User as, String method, String path, Object body) throws Exception {
        return call(as, method, path, body).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
    }

    JsonNode transport(User as, String orderId, Map<String, Object> body) throws Exception {
        return ok(as, "POST", "/orders/" + orderId + "/transporter-events", body);
    }

    JsonNode transport(User as, String orderId, String event) throws Exception {
        return transport(as, orderId, Map.of("eventType", event, "latitude", 7.0, "longitude", 80.0));
    }

    JsonNode buyerEvent(String orderId, String event, String method, String reason) throws Exception {
        Map<String, String> body = new HashMap<>(Map.of("event", event));
        if (method != null) body.put("method", method);
        if (reason != null) body.put("reason", reason);
        return ok(buyer, "POST", "/orders/" + orderId + "/events", body);
    }

    /** Agreed and advance paid by card → dispatched. Returns the order id. */
    String paidOrder() throws Exception {
        Listing l = new Listing();
        l.setFarmerId(farmer.getId());
        l.setFarmerName(farmer.getFullName());
        l.setCropName("Carrots");
        l.setQuantity("1000");
        l.setPrice("150");
        l.setStatus("ACTIVE");
        l.setGrade("A");
        l.setLocation("Dambulla");
        l = listings.save(l);
        String id = ok(buyer, "POST", "/orders/create", Map.of("listingId", l.getId(), "quantity", 400, "pricePerKg", 150))
                .path("orderId").asText();
        String date = LocalDate.now().plusDays(2).toString();
        ok(farmer, "POST", "/orders/" + id + "/confirm-deal", Map.of("quantity", 400, "pricePerKg", 150, "deliveryDate", date));
        ok(buyer, "POST", "/orders/" + id + "/confirm-deal", Map.of("quantity", 400, "pricePerKg", 150, "deliveryDate", date));
        double due = ok(buyer, "GET", "/orders/" + id, null).path("payment").path("advanceDue").asDouble();
        ok(buyer, "POST", "/orders/" + id + "/payment", Map.of("amount", due, "method", "CARD"));
        return id;
    }

    /** Driver accepted, farmer harvested, driver at the farm with 2 photos approved by the buyer. */
    String loadedOrder() throws Exception {
        String id = paidOrder();
        transport(driver, id, "ACCEPT");
        ok(farmer, "POST", "/orders/" + id + "/events", Map.of("event", "START_HARVESTING"));
        ok(farmer, "POST", "/orders/" + id + "/events", Map.of("event", "HARVEST_READY"));
        transport(driver, id, "ARRIVED_PICKUP");
        transport(driver, id, Map.of("eventType", "LOADING_EVIDENCE_SUBMITTED",
                "photoUrls", List.of("/uploads/a.jpg", "/uploads/b.jpg"), "latitude", 7.8742, "longitude", 80.6511));
        buyerEvent(id, "APPROVE_LOADING", null, null);
        return id;
    }

    // ---------------------------------------------------------------- sign-up (vehicle + base + licence)

    @Test
    void driverSignUpNeedsVehicleAndBasePin() throws Exception {
        JsonNode types = ok(null, "GET", "/api/vehicle-types", null);
        assertEquals(4, types.size());
        assertEquals("THREE_WHEELER", types.get(0).path("type").asText());

        Map<String, Object> body = new HashMap<>(Map.of("fullName", "New Driver", "mobileNumber", "0771234567",
                "nic", "199012345678", "email", "new.driver@test.harvesthub.lk", "password", "Secret#123", "role", "TRANSPORTER"));
        String err = bad(null, "POST", "/api/auth/register", body);
        assertTrue(err.contains("Vehicle address"), err);
        body.put("farmLocation", Map.of("latitude", 7.95, "longitude", 80.76, "address", "No. 45, Main Street, Dambulla"));
        assertTrue(bad(null, "POST", "/api/auth/register", body).contains("vehicle type"));
        body.put("vehicleType", "MINI_LORRY");
        body.put("vehiclePlate", "nonsense plate");
        assertTrue(bad(null, "POST", "/api/auth/register", body).contains("plate"));
        body.put("vehiclePlate", "wp nd-4892");
        body.put("vehicleBodyType", "REEFER");
        ok(null, "POST", "/api/auth/register", body);

        User created = users.findByEmail("new.driver@test.harvesthub.lk").orElseThrow();
        assertEquals("MINI_LORRY", created.getVehicleType());
        assertEquals("WP ND-4892", created.getVehiclePlate());
        assertEquals("REEFER", created.getVehicleBodyType());
        assertEquals(7.95, created.getLastLatitude(), 0.0001);
        assertNotEquals(Boolean.FALSE, created.getOnline());

        // licence photo after login, only our own upload paths
        bad(created, "PUT", "/api/me/vehicle", Map.of("licenceUrl", "https://evil.example/x.jpg"));
        bad(created, "PUT", "/api/me/vehicle", Map.of("vehicleCapacityKg", 1500));   // above mini lorry class
        JsonNode me = ok(created, "PUT", "/api/me/vehicle", Map.of("licenceUrl", "/uploads/licence.jpg", "vehicleCapacityKg", 900));
        assertTrue(me.path("licenceUploaded").asBoolean());
        assertEquals(900, me.path("vehicleCapacityKg").asDouble(), 0.01);
        call(buyer, "PUT", "/api/me/vehicle", Map.of("vehiclePlate", "WP ND-4892")).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- online switch + 10-min request expiry

    @Test
    void offlineDriversGetNoJobsAndUnansweredRequestsMoveOn() throws Exception {
        ok(driver, "PUT", "/api/me/availability", Map.of("online", false));
        ok(driver2, "PUT", "/api/me/availability", Map.of("online", false));
        String id = paidOrder();
        assertEquals("PAUSED", orders.findById(id).orElseThrow().getStatus());

        ok(driver, "PUT", "/api/me/availability", Map.of("online", true, "latitude", 7.87, "longitude", 80.65));
        ok(driver2, "PUT", "/api/me/availability", Map.of("online", true));
        dispatchService.retryPaused();
        Order o = orders.findById(id).orElseThrow();
        assertEquals("TRANSPORTER_ASSIGNED", o.getStatus());
        // broadcast to every nearby matching driver, nearest first; nobody owns it until someone accepts
        assertNull(o.getTransporterId());
        assertEquals(driver.getId(), o.getOfferedTransporterIds().get(0));
        assertTrue(o.getOfferedTransporterIds().contains(driver2.getId()));
        assertNotNull(o.getTransportRequestExpiresAt());
        assertTrue(o.getTransportRequestExpiresAt() > System.currentTimeMillis() + 9 * 60_000L);

        JsonNode d = ok(driver, "GET", "/dashboard/transporter/" + driver.getId(), null);
        assertEquals(1, d.path("pendingRequests").size());
        JsonNode job = d.path("pendingRequests").get(0);
        assertEquals(id, job.path("orderId").asText());
        assertEquals(job.path("transportFee").asDouble() * 0.95, job.path("payout").asDouble(), 0.01);
        assertTrue(job.path("distanceKm").asDouble() > 0);
        call(driver2, "GET", "/dashboard/transporter/" + driver.getId(), null).andExpect(status().isForbidden());

        // 10 minutes pass without an answer
        o.setTransportRequestExpiresAt(System.currentTimeMillis() - 1000);
        orders.save(o);
        String err = bad(driver, "POST", "/orders/" + id + "/transporter-events", Map.of("eventType", "ACCEPT"));
        assertTrue(err.contains("expired"), err);
        orderService.expireTransportRequests();
        o = orders.findById(id).orElseThrow();
        assertEquals("PAUSED", o.getStatus()); // both drivers let it expire
        assertTrue(o.getOfferedTransporterIds().isEmpty());
        assertEquals(0, ok(driver, "GET", "/dashboard/transporter/" + driver.getId(), null).path("pendingRequests").size());

        // the retries give them another chance; the first driver to accept wins
        dispatchService.retryPaused();
        dispatchService.retryPaused();
        o = orders.findById(id).orElseThrow();
        assertEquals("TRANSPORTER_ASSIGNED", o.getStatus());
        assertEquals(2, o.getOfferedTransporterIds().size());
        assertEquals(1, ok(driver2, "GET", "/dashboard/transporter/" + driver2.getId(), null).path("pendingRequests").size());
        assertEquals("TRANSPORTER_ACCEPTED", transport(driver2, id, "ACCEPT").path("status").asText());
        String late = bad(driver, "POST", "/orders/" + id + "/transporter-events", Map.of("eventType", "ACCEPT"));
        assertTrue(late.contains("first"), late);
        o = orders.findById(id).orElseThrow();
        assertEquals(driver2.getId(), o.getTransporterId());
        assertNotNull(o.getTransporterName());
        assertTrue(o.getOfferedTransporterIds().isEmpty());
    }

    // ---------------------------------------------------------------- pre-loading photos 2..4

    @Test
    void preLoadingNeedsTwoToFourPhotos() throws Exception {
        String id = paidOrder();
        transport(driver, id, "ACCEPT");
        ok(farmer, "POST", "/orders/" + id + "/events", Map.of("event", "START_HARVESTING"));
        ok(farmer, "POST", "/orders/" + id + "/events", Map.of("event", "HARVEST_READY"));
        transport(driver, id, "ARRIVED_PICKUP");
        String path = "/orders/" + id + "/transporter-events";
        assertTrue(bad(driver, "POST", path, Map.of("eventType", "LOADING_EVIDENCE_SUBMITTED",
                "photoUrls", List.of("/uploads/a.jpg"))).contains("at least 2"));
        assertTrue(bad(driver, "POST", path, Map.of("eventType", "LOADING_EVIDENCE_SUBMITTED",
                "photoUrls", List.of("/uploads/1.jpg", "/uploads/2.jpg", "/uploads/3.jpg", "/uploads/4.jpg", "/uploads/5.jpg"))).contains("at most 4"));
        // Start Journey stays locked until the buyer approves
        bad(driver, "POST", path, Map.of("eventType", "START_JOURNEY"));
        JsonNode o = transport(driver, id, Map.of("eventType", "LOADING_EVIDENCE_SUBMITTED",
                "photoUrls", List.of("/uploads/a.jpg", "/uploads/b.jpg"), "latitude", 7.8742, "longitude", 80.6511));
        assertEquals("AWAITING_BUYER_LOADING_APPROVAL", o.path("status").asText());
        bad(driver, "POST", path, Map.of("eventType", "START_JOURNEY"));
    }

    // ---------------------------------------------------------------- happy path: payout, earnings, rating

    @Test
    void completedTripPaysTheDriverAndShowsOnTheDashboard() throws Exception {
        String id = loadedOrder();
        JsonNode busy = ok(driver, "GET", "/dashboard/transporter/" + driver.getId(), null);
        assertTrue(busy.path("busy").asBoolean());
        assertEquals(id, busy.path("activeJob").path("orderId").asText());

        transport(driver, id, "START_JOURNEY");
        transport(driver, id, "LOCATION");
        assertTrue(ok(driver, "GET", "/orders/" + id + "/tracking", null).path("remainingKm").asDouble() >= 0);
        transport(driver, id, "ARRIVED_DESTINATION");
        bad(driver, "POST", "/orders/" + id + "/transporter-events", Map.of("eventType", "DELIVERY_COMPLETED")); // buyer must accept first
        buyerEvent(id, "ACCEPT_DELIVERY", "CARD", null);
        assertEquals("DELIVERED", transport(driver, id, "DELIVERY_COMPLETED").path("status").asText());
        assertEquals("COMPLETED", buyerEvent(id, "GOODS_RECEIVED", null, null).path("status").asText());

        Order o = orders.findById(id).orElseThrow();
        double payout = o.getPayment().getTransportFee() * 0.95;
        assertEquals(payout, o.getPayment().getTransporterPayout(), 0.01);
        assertEquals(payout, ledger.wallet(driver.getId()).getBalance(), 0.01);

        JsonNode d = ok(driver, "GET", "/dashboard/transporter/" + driver.getId(), null);
        assertFalse(d.path("busy").asBoolean());
        assertEquals(1, d.path("tripsCompleted").asInt());
        assertEquals(payout, d.path("earningsTotal").asDouble(), 0.01);
        assertEquals(payout, d.path("earningsToday").asDouble(), 0.01);
        assertEquals(o.getPayment().getTransportFee() * 0.05, d.path("commissionTotal").asDouble(), 0.01);
        assertEquals("TRANSPORT_PAYOUT", d.path("recentPayouts").get(0).path("type").asText());
        assertNotEquals(Boolean.FALSE, users.findById(driver.getId()).orElseThrow().getAvailable());

        ok(driver, "POST", "/orders/" + id + "/rate", Map.of("rating", 5, "toRole", "FARMER", "comment", "Fresh Harvest, On-Time Loading"));
        ok(driver, "POST", "/orders/" + id + "/rate", Map.of("rating", 4, "toRole", "BUYER", "comment", "Fast Unloading"));
        bad(driver, "POST", "/orders/" + id + "/rate", Map.of("rating", 4, "toRole", "BUYER"));
    }

    // ---------------------------------------------------------------- post-delivery rejection, return, appeal, suspension

    @Test
    void rejectedDeliveryChargesTheDriverWhoReturnsTheGoodsAndCanAppeal() throws Exception {
        String id = loadedOrder();
        transport(driver, id, "START_JOURNEY");
        transport(driver, id, "ARRIVED_DESTINATION");
        assertEquals("RETURN_JOURNEY", buyerEvent(id, "REJECT_DELIVERY", null, "Crushed crates").path("status").asText());

        Order o = orders.findById(id).orElseThrow();
        double charge = o.getPayment().getAdvanceCropPortion();
        JsonNode d = ok(driver, "GET", "/dashboard/transporter/" + driver.getId(), null);
        assertTrue(d.path("suspended").asBoolean());
        assertEquals(charge, d.path("amountOwed").asDouble(), 0.01);
        assertEquals(charge, d.path("damageChargesTotal").asDouble(), 0.01);
        assertEquals(1, d.path("rejectedDeliveries").asInt());
        assertTrue(d.path("busy").asBoolean(), "return trip is still to drive");
        assertEquals(Boolean.FALSE, users.findById(driver.getId()).orElseThrow().getAvailable());

        // appeal = ticket only; buyer cannot use it at this stage; charge unchanged
        assertTrue(bad(buyer, "POST", "/orders/" + id + "/disputes", Map.of("reason", "x")).contains("reject the delivery"));
        JsonNode appeal = ok(driver, "POST", "/orders/" + id + "/disputes", Map.of("reason", "Unloading Handling Damage",
                "details", "Approved photos show intact crates", "photoUrls", List.of("/uploads/slip.jpg")));
        assertEquals("APPEAL", appeal.path("type").asText());
        assertEquals("OPEN", appeal.path("status").asText());
        assertEquals(1, appeal.path("photoUrls").size());
        assertEquals(-charge, ledger.wallet(driver.getId()).getBalance(), 0.01);

        assertEquals("RETURNED", transport(driver, id, "GOODS_RETURNED").path("status").asText());
        assertEquals(Boolean.TRUE, users.findById(driver.getId()).orElseThrow().getAvailable());
        bad(driver, "POST", "/orders/" + id + "/transporter-events", Map.of("eventType", "GOODS_RETURNED"));

        // suspended drivers get no jobs until they pay back (mock top-up)
        ok(driver2, "PUT", "/api/me/availability", Map.of("online", false));
        String next = paidOrder();
        assertEquals("PAUSED", orders.findById(next).orElseThrow().getStatus());
        ok(driver, "POST", "/wallet/" + driver.getId() + "/deposit", Map.of("amount", charge));
        assertFalse(ok(driver, "GET", "/dashboard/transporter/" + driver.getId(), null).path("suspended").asBoolean());
        dispatchService.retryPaused();
        assertEquals(List.of(driver.getId()), orders.findById(next).orElseThrow().getOfferedTransporterIds());
    }
}
