package com.harvesthub.backend.service;

import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Registered so Spring Boot does not create its default in-memory user.
 * Authentication itself is done by {@link UserService#authenticate} and the JWT filter.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email == null ? "" : email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String role = user.getRole() == null ? "FARMER" : user.getRole();
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPassword(), List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
