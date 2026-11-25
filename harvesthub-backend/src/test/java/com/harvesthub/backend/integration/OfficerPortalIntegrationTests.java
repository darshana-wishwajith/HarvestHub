package com.harvesthub.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harvesthub.backend.model.*;
import com.harvesthub.backend.repository.*;
import com.harvesthub.backend.service.DisputeService;
import com.harvesthub.backend.util.JwtUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Agricultural Officer desktop portal (four roles only, officers verify listings and monitor
 * users / orders / disputes) over HTTP on the LOCAL mongodb://localhost:27017/harvesthub_it database. Never touches Atlas.
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
class OfficerPortalIntegrationTests {

    static final String PASSWORD = "Officer-test-1";

    @Autowired MockMvc mvc;
    @Autowired MongoTemplate mongo;
    @Autowired JwtUtil jwt;
    @Autowired UserRepository users;
    @Autowired ListingRepository listings;
    @Autowired OrderRepository orders;
    @Autowired NotificationRepository notifications;
    @Autowired DisputeService disputeService;
    @Autowired PasswordEncoder encoder;

    final ObjectMapper json = new ObjectMapper();
    User officer, officer2, farmer, buyer, driver;

    @BeforeEach
    void freshDatabase() {
        assertTrue(mongo.getDb().getName().endsWith("_it"), "integration tests must use the local _it database");
        mongo.getDb().drop();
        officer = user("op-officer", "OFFICER");
        officer2 = user("op-officer2", "OFFICER");
        farmer = user("op-farmer", "FARMER");
        buyer = user("op-buyer", "BUYER");
        driver = user("op-driver", "TRANSPORTER");
    }

    @AfterAll
    static void cleanup(@Autowired MongoTemplate mongo) {
        mongo.getDb().drop();
    }

    // ------------------------------------------------------------------ fixtures

    User user(String id, String role) {
        User u = new User();
        u.setId(id);
        String rest = id.substring(3);
        u.setFullName("Test " + Character.toUpperCase(rest.charAt(0)) + rest.substring(1).replace("2", " Two"));
        u.setEmail(id + "@test.harvesthub.lk");
        u.setMobileNumber("07" + Math.abs(id.hashCode() % 100_000_000));
        u.setNic("IT" + id);
        u.setPassword(encoder.encode(PASSWORD));
        u.setRole(role);
        FarmLocation loc = new FarmLocation();
        loc.setLatitude(7.8742);
        loc.setLongitude(80.6511);
        loc.setAddress("Dambulla");
        u.setFarmLocation(loc);
        return users.save(u);
    }

    Listing pendingListing(String crop, String createdAt) {
        Listing l = new Listing();
        l.setFarmerId(farmer.getId());
        l.setFarmerName(farmer.getFullName());
        l.setCropName(crop);
        l.setQuantity("450");
        l.setPrice("120");
        l.setLocation("Dambulla");
        l.setStatus("PENDING");
        l.setGpsLatitude(7.8743);              // ~11 m from the farm
        l.setGpsLongitude(80.6511);
        l.setMediaUrls(List.of("/uploads/a.jpg", "/uploads/b.jpg", "/uploads/c.mp4"));
        l.setCreatedDate(createdAt);
        l.setUpdatedTime(createdAt);
        l.setCapturedAt(createdAt);
        return listings.save(l);
    }

    Order order(String id, String status, boolean withDriver) {
        Order o = new Order();
        o.setOrderId(id);
        o.setFarmerId(farmer.getId());
        o.setBuyerId(buyer.getId());
        if (withDriver) o.setTransporterId(driver.getId());
        o.setCropName("Carrots");
        o.setQuantity(500);
        o.setPricePerKg(150);
        o.setStatus(status);
        o.setCreatedDate(LocalDateTime.now().toString());
        o.setStatusHistory(new ArrayList<>());
        return orders.save(o);
    }

    ResultActions call(User as, String method, String path, Object body) throws Exception {
        var req = "GET".equals(method) ? get(path) : post(path);
        if (as != null) req.header("Authorization", "Bearer " + jwt.generateToken(as));
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return mvc.perform(req);
    }

    JsonNode ok(User as, String method, String path, Object body) throws Exception {
        return json.readTree(call(as, method, path, body).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    String fails(User as, String method, String path, Object body, int code) throws Exception {
        return call(as, method, path, body).andExpect(status().is(code)).andReturn().getResponse().getContentAsString();
    }

    ResultActions login(User u) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", u.getEmail(), "password", PASSWORD))));
    }

    // ------------------------------------------------------------------ tests

    @Test
    void onlyOfficersReachThePortal() throws Exception {
        for (String path : new String[]{"/api/queue", "/api/dashboard/stats", "/api/officer/users", "/api/officer/orders",
                "/api/officer/disputes", "/api/review-history"}) {
            call(farmer, "GET", path, null).andExpect(status().isForbidden());
            call(buyer, "GET", path, null).andExpect(status().isForbidden());
            int anonymous = call(null, "GET", path, null).andReturn().getResponse().getStatus();
            assertTrue(anonymous == 401 || anonymous == 403, path + " without a token gave " + anonymous);
            call(officer, "GET", path, null).andExpect(status().isOk());
        }
        // login answers with the officer profile for the desktop; officer code comes from the NIC
        JsonNode body = json.readTree(login(officer).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals("OFFICER", body.get("officer").get("role").asText());
        assertEquals("HH-ICER", body.get("officer").get("officerIdCode").asText());
    }

    @Test
    void sharedQueueClaimLockReleaseAndGrading() throws Exception {
        Listing older = pendingListing("Carrots", "2026-11-14T08:00:00");
        Listing newer = pendingListing("Leeks", "2026-11-15T09:30:00");

        JsonNode queue = ok(officer, "GET", "/api/queue", null);
        assertEquals(2, queue.size());
        assertEquals(older.getId(), queue.get(0).get("id").asText(), "first come, first served");
        assertEquals(450, queue.get(0).get("quantityKg").asDouble());
        assertEquals(3, queue.get(0).get("mediaCount").asInt());

        // officer 1 claims; officer 2 is locked out and cannot grade it
        JsonNode claimed = ok(officer, "POST", "/api/queue/" + older.getId() + "/claim", null);
        assertTrue(claimed.get("claimedByMe").asBoolean());
        assertTrue(fails(officer2, "POST", "/api/queue/" + older.getId() + "/claim", null, 409).contains("Test Officer"));
        fails(officer2, "POST", "/api/listings/" + older.getId() + "/review", Map.of("decision", "APPROVE", "grade", "A"), 409);
        assertEquals(1, ok(officer, "GET", "/api/queue?tab=claimed", null).size());
        assertEquals(1, ok(officer2, "GET", "/api/queue?tab=unclaimed", null).size());

        // authenticity checks come from the listing's own GPS + media
        JsonNode checks = claimed.get("checks");
        assertTrue(checks.get(0).get("ok").asBoolean(), checks.toString());
        assertTrue(checks.get(1).get("ok").asBoolean(), checks.toString());

        // a review left without an officer (older data) can still be taken over
        Listing orphan = pendingListing("Beans", "2026-11-13T07:00:00");
        orphan.setStatus("UNDER_REVIEW");
        listings.save(orphan);
        assertTrue(ok(officer2, "POST", "/api/queue/" + orphan.getId() + "/claim", null).get("claimedByMe").asBoolean());
        ok(officer2, "POST", "/api/queue/" + orphan.getId() + "/release", null);
        listings.deleteById(orphan.getId());

        // release puts it back in the shared queue; officer 2 can now take it
        fails(officer2, "POST", "/api/queue/" + older.getId() + "/release", null, 409);
        ok(officer, "POST", "/api/queue/" + older.getId() + "/release", null);
        ok(officer2, "POST", "/api/queue/" + older.getId() + "/claim", null);

        // grading rules: grade is mandatory on approval, a reason on retake / reject
        assertTrue(fails(officer2, "POST", "/api/listings/" + older.getId() + "/review", Map.of("decision", "APPROVE"), 400).contains("Grade"));
        fails(officer2, "POST", "/api/listings/" + older.getId() + "/review", Map.of("decision", "REJECT", "notes", ""), 400);
        JsonNode approved = ok(officer2, "POST", "/api/listings/" + older.getId() + "/review",
                Map.of("decision", "APPROVE", "grade", "b", "notes", "Uniform colour"));
        assertEquals("ACTIVE", approved.get("status").asText());
        assertEquals("B", approved.get("grade").asText());
        assertEquals(1, approved.get("reviews").size());
        assertTrue(notifications.findByFarmerIdOrderByTimestampDesc(farmer.getId()).stream()
                .anyMatch(n -> n.getTitle().contains("approved")));

        // retake, then the farmer re-submits: the queue marks it, history keeps both decisions
        ok(officer, "POST", "/api/queue/" + newer.getId() + "/claim", null);
        ok(officer, "POST", "/api/listings/" + newer.getId() + "/review", Map.of("decision", "RETAKE", "notes", "Close-up is blurred"));
        Listing again = listings.findById(newer.getId()).orElseThrow();
        again.setStatus("PENDING");
        listings.save(again);
        assertTrue(ok(officer, "GET", "/api/queue", null).get(0).get("resubmitted").asBoolean());
        ok(officer, "POST", "/api/queue/" + newer.getId() + "/claim", null);
        ok(officer, "POST", "/api/listings/" + newer.getId() + "/review", Map.of("decision", "REJECT", "notes", "Leaves show fungal damage"));

        JsonNode mine = ok(officer, "GET", "/api/review-history", null);
        assertEquals(2, mine.size());
        assertEquals("REJECTED", mine.get(0).get("decision").asText(), "newest first");
        assertEquals(3, ok(officer, "GET", "/api/review-history?scope=all", null).size());
        assertEquals(1, ok(officer, "GET", "/api/review-history?scope=all&decision=APPROVED", null).size());

        JsonNode stats = ok(officer, "GET", "/api/dashboard/stats", null);
        assertEquals(0, stats.get("pendingCount").asInt());
        assertEquals(2, stats.get("myReviewedTotal").asInt());
        assertEquals(0, stats.get("approvalRate").asInt());
        assertEquals(3, stats.get("reviewedTodayCount").asInt());
        assertEquals(100, ok(officer2, "GET", "/api/dashboard/stats", null).get("approvalRate").asInt());
        assertTrue(ok(officer, "GET", "/api/dashboard/activity", null).size() >= 5);
    }

    @Test
    void officerSuspendsAndReactivatesAccounts() throws Exception {
        assertEquals(1, ok(officer, "GET", "/api/officer/users?role=FARMER", null).size());
        assertEquals(5, ok(officer, "GET", "/api/officer/users", null).size());
        assertEquals(1, ok(officer, "GET", "/api/officer/users?search=op-buyer", null).size());

        assertTrue(fails(officer, "POST", "/api/officer/users/" + officer2.getId() + "/suspend",
                Map.of("reason", "Testing officer suspension"), 400).contains("cannot be suspended"));
        fails(officer, "POST", "/api/officer/users/" + officer.getId() + "/suspend", Map.of("reason", "Suspending myself now"), 400);
        fails(officer, "POST", "/api/officer/users/" + driver.getId() + "/suspend", Map.of("reason", "short"), 400);

        login(driver).andExpect(status().isOk());
        JsonNode detail = ok(officer, "POST", "/api/officer/users/" + driver.getId() + "/suspend",
                Map.of("reason", "Repeated damaged deliveries reported by buyers"));
        assertTrue(detail.get("user").get("accountSuspended").asBoolean());
        assertEquals("Test Officer", detail.get("suspendedByName").asText());
        assertFalse(users.findById(driver.getId()).orElseThrow().getOnline(), "suspended drivers go offline");
        assertTrue(login(driver).andExpect(status().isForbidden()).andReturn().getResponse().getContentAsString()
                .contains("Repeated damaged deliveries"));
        int code = call(driver, "GET", "/dashboard/transporter/" + driver.getId(), null).andReturn().getResponse().getStatus();
        assertTrue(code == 401 || code == 403, "an old token stops working at once, got " + code);
        assertEquals(1, ok(officer, "GET", "/api/officer/users?status=suspended", null).size());
        assertEquals(1, ok(officer, "GET", "/api/dashboard/stats", null).get("suspendedUsers").asInt());

        ok(officer, "POST", "/api/officer/users/" + driver.getId() + "/reactivate", Map.of("note", "Checked with the buyers"));
        login(driver).andExpect(status().isOk());
        fails(officer, "POST", "/api/officer/users/" + driver.getId() + "/reactivate", null, 400);
        assertTrue(notifications.findByFarmerIdOrderByTimestampDesc(driver.getId()).stream()
                .anyMatch(n -> "Account reactivated".equals(n.getTitle())));
        fails(officer, "GET", "/api/officer/users/nobody", null, 404);
    }

    @Test
    void ordersAreGroupedForMonitoring() throws Exception {
        order("ORD-A", "IN_TRANSIT", true);
        order("ORD-B", "COMPLETED", true);
        order("ORD-C", "PRELOAD_REJECTED", true);
        Order disputed = order("ORD-D", "COMPLETED", true);
        disputed.setDisputeOpen(true);
        orders.save(disputed);

        assertEquals(4, ok(officer, "GET", "/api/officer/orders", null).size());
        assertEquals(1, ok(officer, "GET", "/api/officer/orders?stage=active", null).size());
        assertEquals(1, ok(officer, "GET", "/api/officer/orders?stage=completed", null).size());
        assertEquals(2, ok(officer, "GET", "/api/officer/orders?stage=problem", null).size());
        assertEquals(1, ok(officer, "GET", "/api/officer/orders?search=ORD-C", null).size());

        JsonNode detail = ok(officer, "GET", "/api/officer/orders/ORD-A", null);
        assertEquals(75_000, detail.get("order").get("cropValue").asDouble());
        assertEquals("Test Farmer", detail.get("farmer").get("fullName").asText());
        assertEquals("TRANSPORTER", detail.get("transporter").get("role").asText());

        JsonNode user = ok(officer, "GET", "/api/officer/users/" + buyer.getId(), null);
        assertEquals(4, user.get("orders").size(), "a user's detail lists every order they are part of");
        JsonNode stats = ok(officer, "GET", "/api/dashboard/stats", null);
        assertEquals(1, stats.get("activeOrders").asInt());
        assertEquals(2, stats.get("problemOrders").asInt());
    }

    @Test
    void officerResolvesAppealsWithANoteAndMoneyStaysTheSame() throws Exception {
        order("ORD-R", "RETURN_JOURNEY", true);
        Dispute appeal = disputeService.open("ORD-R", driver.getId(), "Damage happened at loading", "Photos attached", List.of("/uploads/x.jpg"));
        assertEquals("APPEAL", appeal.getType());
        assertTrue(notifications.findByFarmerIdOrderByTimestampDesc(officer.getId()).stream()
                .anyMatch(n -> n.getTitle().startsWith("New dispute")), "officers are told about new disputes");

        JsonNode open = ok(officer, "GET", "/api/officer/disputes?status=open", null);
        assertEquals(1, open.size());
        assertEquals("RETURN_JOURNEY", open.get(0).get("orderStatus").asText());
        assertEquals(1, ok(officer, "GET", "/api/dashboard/stats", null).get("openDisputes").asInt());
        JsonNode detail = ok(officer, "GET", "/api/officer/disputes/" + appeal.getId(), null);
        assertEquals("TRANSPORTER", detail.get("raisedBy").get("role").asText());

        fails(officer, "POST", "/api/officer/disputes/" + appeal.getId() + "/resolve", Map.of("outcome", "RESOLVED", "note", "ok"), 400);
        fails(officer, "POST", "/api/officer/disputes/" + appeal.getId() + "/resolve", Map.of("outcome", "MAYBE", "note", "Not a valid outcome"), 400);
        double before = orders.findById("ORD-R").orElseThrow().getTotalAmount();
        JsonNode closed = ok(officer, "POST", "/api/officer/disputes/" + appeal.getId() + "/resolve",
                Map.of("outcome", "DISMISSED", "note", "Loading photos show the crop was intact when it left the farm"));
        assertEquals("DISMISSED", closed.get("dispute").get("status").asText());
        assertEquals("Test Officer", closed.get("dispute").get("resolvedByName").asText());
        Order after = orders.findById("ORD-R").orElseThrow();
        assertFalse(after.isDisputeOpen());
        assertEquals(before, after.getTotalAmount());
        assertTrue(notifications.findByFarmerIdOrderByTimestampDesc(driver.getId()).stream()
                .anyMatch(n -> n.getTitle().equals("Your appeal was dismissed")));
        assertTrue(notifications.findByFarmerIdOrderByTimestampDesc(buyer.getId()).stream()
                .anyMatch(n -> n.getBody().contains("Officer decision")));
        fails(officer, "POST", "/api/officer/disputes/" + appeal.getId() + "/resolve",
                Map.of("outcome", "RESOLVED", "note", "Trying to close it twice"), 400);
        assertEquals(1, ok(officer, "GET", "/api/officer/disputes?status=closed", null).size());
    }

    // ------------------------------------------------------------------ BUG-18: routing errors are client errors, not 500

    @Test
    void unknownUrlsAndWrongMethodsAreClientErrorsNot500() throws Exception {
        String auth = "Bearer " + jwt.generateToken(farmer);
        mvc.perform(get("/api/no-such-endpoint").header("Authorization", auth))
                .andExpect(status().isNotFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.error").exists());
        // /listings/{id} only has PUT and DELETE
        mvc.perform(get("/listings/abc").header("Authorization", auth)).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/dashboard/stats").header("Authorization", "Bearer " + jwt.generateToken(officer)))
                .andExpect(status().isMethodNotAllowed());
        // a missing query parameter is the caller's mistake too
        mvc.perform(get("/listings/location-check").header("Authorization", auth)).andExpect(status().isBadRequest());
        // without a token the security rules still answer first
        mvc.perform(get("/api/no-such-endpoint")).andExpect(status().isUnauthorized());
    }
}
