package com.harvesthub.backend.service;

import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Forgot-password: send code -> verify code -> set new password. Pure unit test, no database. */
class PasswordResetFlowTests {

    private static final String EMAIL = "farmer@test.harvesthub.lk";
    private static final String MOBILE = "0771234567";

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4); // fast for tests
    private UserRepository repo;
    private EmailService email;
    private UserService service;
    private User user;

    @BeforeEach
    void setUp() {
        repo = mock(UserRepository.class);
        email = mock(EmailService.class);
        service = new UserService(repo, encoder, email);
        user = new User();
        user.setId("u1");
        user.setEmail(EMAIL);
        user.setMobileNumber(MOBILE);
        user.setPassword(encoder.encode("OldPass123"));
        when(repo.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(repo.findByMobileNumber(MOBILE)).thenReturn(Optional.of(user));
    }

    private String requestCode(String identifier) {
        service.createPasswordResetTokenForUser(identifier);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(email, atLeastOnce()).sendPasswordResetEmail(eq(EMAIL), code.capture());
        return code.getValue();
    }

    @Test
    void fullFlowByMobileNumber() {
        String code = requestCode(MOBILE);
        assertTrue(code.matches("\\d{6}"));
        assertNotEquals(code, user.getResetPasswordToken(), "only a hash may be stored");

        service.verifyResetCode(MOBILE, code);          // does not use the code up
        service.verifyResetCode(MOBILE, code);
        service.resetPassword(MOBILE, code, "NewPass456");

        assertTrue(encoder.matches("NewPass456", user.getPassword()));
        assertNull(user.getResetPasswordToken());
        assertThrows(IllegalStateException.class, () -> service.verifyResetCode(MOBILE, code), "code is single-use");
    }

    @Test
    void wrongCodeIsRejected() {
        String code = requestCode(EMAIL);
        String wrong = code.equals("000000") ? "111111" : "000000";
        assertThrows(IllegalStateException.class, () -> service.verifyResetCode(EMAIL, wrong));
        service.verifyResetCode(EMAIL, code); // the right one still works after one mistake
    }

    @Test
    void fiveWrongGuessesBurnTheCode() {
        String code = requestCode(EMAIL);
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            assertThrows(IllegalStateException.class, () -> service.verifyResetCode(EMAIL, wrong));
        }
        assertThrows(IllegalStateException.class, () -> service.verifyResetCode(EMAIL, code));
    }

    @Test
    void expiredCodeIsRejected() {
        String code = requestCode(EMAIL);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().minusSeconds(1));
        assertThrows(IllegalStateException.class, () -> service.verifyResetCode(EMAIL, code));
    }

    @Test
    void newPasswordMustDifferFromCurrent() {
        String code = requestCode(EMAIL);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.resetPassword(EMAIL, code, "OldPass123"));
        assertTrue(e.getMessage().contains("different"));
        assertNotNull(user.getResetPasswordToken(), "code stays valid so the user can pick another password");
    }

    @Test
    void unknownAccountGetsNoEmailAndNoError() {
        service.createPasswordResetTokenForUser("nobody@test.harvesthub.lk");
        verify(email, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void secondRequestWithinAMinuteDoesNotSendAnotherEmail() {
        requestCode(EMAIL);
        service.createPasswordResetTokenForUser(EMAIL);
        verify(email, times(1)).sendPasswordResetEmail(anyString(), anyString());

        // after the cooldown a new code is issued
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plus(UserService.RESET_CODE_VALIDITY).minusMinutes(2));
        service.createPasswordResetTokenForUser(EMAIL);
        verify(email, times(2)).sendPasswordResetEmail(anyString(), anyString());
    }
}
