package com.harvesthub.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Security rules that must hold without any database access. */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void protectedEndpointsRequireAToken() throws Exception {
        for (String path : new String[]{"/api/queue", "/api/users", "/api/review-history", "/buyer/listings",
                "/orders/farmer/x", "/wallet/x/summary", "/notifications/x", "/api/officers/me"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void forgedTokenIsRejected() throws Exception {
        mvc.perform(get("/buyer/listings").header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.e30.forged"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cannotSelfRegisterAsAdminOrOfficer() throws Exception {
        for (String role : new String[]{"ADMIN", "OFFICER"}) {
            String body = """
                    {"fullName":"Probe","mobileNumber":"0700000000","nic":"200000000000",
                     "email":"probe@example.test","password":"ProbePass#1","role":"%s"}""".formatted(role);
            mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void registrationValidatesInput() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPasswordStepsArePublicAndValidateInput() throws Exception {
        // 400 (validation) rather than 401 proves the route is public; no database access happens.
        mvc.perform(post("/api/auth/verify-reset-code").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"probe@example.test\",\"code\":\"12ab\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"probe@example.test\",\"token\":\"123456\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removedDebugAndProfileEndpointsAreNotPublic() throws Exception {
        mvc.perform(get("/api/auth/darshana")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/farmer/someone@example.test/profile")).andExpect(status().isUnauthorized());
    }
}
