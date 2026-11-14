package com.harvesthub.backend.security;

import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.UserRepository;
import com.harvesthub.backend.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The forgot-password flow over HTTP, exactly as the Android app calls it, with no token:
 * forgot-password -> verify-reset-code -> reset-password -> login. Database and mail are mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetEndpointTests {

    private static final String EMAIL = "farmer@test.harvesthub.lk";

    @Autowired private MockMvc mvc;
    @Autowired private PasswordEncoder encoder;
    @MockBean private UserRepository repo;
    @MockBean private EmailService email;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("reset-test-user");
        user.setEmail(EMAIL);
        user.setMobileNumber("0771234567");
        user.setRole(Roles.FARMER);
        user.setPassword(encoder.encode("OldPass123"));
        when(repo.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(repo.findByMobileNumber("0771234567")).thenReturn(Optional.of(user));
        when(repo.findById(user.getId())).thenReturn(Optional.of(user));
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void postJson(String path, String json, int expectedStatus) throws Exception {
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void fullFlowWorksWithoutALoginToken() throws Exception {
        postJson("/api/auth/forgot-password", "{\"email\":\"0771234567\"}", 200);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(email).sendPasswordResetEmail(eq(EMAIL), code.capture());

        String wrong = code.getValue().equals("000000") ? "111111" : "000000";
        postJson("/api/auth/verify-reset-code",
                "{\"email\":\"0771234567\",\"code\":\"" + wrong + "\"}", 400);
        postJson("/api/auth/verify-reset-code",
                "{\"email\":\"0771234567\",\"code\":\"" + code.getValue() + "\"}", 200);
        // The Android app sends the code as "resetToken".
        postJson("/api/auth/reset-password",
                "{\"email\":\"0771234567\",\"resetToken\":\"" + code.getValue() + "\",\"newPassword\":\"NewPass456\"}", 200);

        assertTrue(encoder.matches("NewPass456", user.getPassword()));
        postJson("/api/auth/login", "{\"emailOrMobile\":\"0771234567\",\"password\":\"NewPass456\"}", 200);
    }

    @Test
    void verifyStepIsPublicEvenWithAStaleTokenHeader() throws Exception {
        mvc.perform(post("/api/auth/verify-reset-code")
                        .header("Authorization", "Bearer stale.or.expired.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"code\":\"123456\"}"))
                .andExpect(status().isBadRequest()); // wrong code -> 400, never 401
    }

    @Test
    void unknownAccountGetsTheSameAnswer() throws Exception {
        postJson("/api/auth/forgot-password", "{\"email\":\"nobody@test.harvesthub.lk\"}", 200);
        verify(email, org.mockito.Mockito.never()).sendPasswordResetEmail(eq("nobody@test.harvesthub.lk"), anyString());
    }
}
