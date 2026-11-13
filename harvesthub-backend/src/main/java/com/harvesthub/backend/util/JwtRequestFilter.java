package com.harvesthub.backend.util;

import com.harvesthub.backend.repository.UserRepository;
import com.harvesthub.backend.security.AuthUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates requests carrying "Authorization: Bearer <jwt>".
 * The user is re-loaded from the database so deleted users and role changes take effect immediately.
 * Requests without a valid token continue unauthenticated; SecurityConfig decides whether that is allowed.
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtRequestFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtUtil.parseValid(header.substring(7))
                    .flatMap(claims -> userRepository.findById(claims.getSubject()))
                    .ifPresent(user -> {
                        String role = user.getRole() == null ? "FARMER" : user.getRole().toUpperCase();
                        AuthUser principal = new AuthUser(user.getId(), user.getEmail(), role, user.getFullName());
                        var auth = new UsernamePasswordAuthenticationToken(
                                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
        }
        chain.doFilter(request, response);
    }
}
