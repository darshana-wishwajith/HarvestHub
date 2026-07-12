package com.harvesthub.backend.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtUtilTests {

    @Test
    void generatesAndParsesTokenWithRawSecret() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "E/VvXRt8V/pD+E8aU4rF3jX+yZz!A%D*G-KaPdSgVkYp3s6v9y$B&E)H@McQfTjW");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 60000L);

        User userDetails = new User("test@example.com", "password", List.of());

        String token = jwtUtil.generateToken(userDetails);

        assertEquals("test@example.com", jwtUtil.extractUsername(token));
    }
}
