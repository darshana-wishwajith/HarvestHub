package com.harvesthub.backend.service;

import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        if (identifier == null || identifier.isBlank()) {
            throw new UsernameNotFoundException("Identifier cannot be empty");
        }

        String trimmed = identifier.trim();

        // 1. Try find by email
        Optional<User> userOptional = userRepository.findByEmail(trimmed);

        // 2. Try find by mobile number
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByMobileNumber(trimmed);
        }

        // 3. Try find by exact ID
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findById(trimmed);
        }

        // 4. Try find by NIC
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByNic(trimmed);
        }

        // 5. Try find by email prefix or username match
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findAll().stream()
                    .filter(u -> (u.getEmail() != null && u.getEmail().split("@")[0].equalsIgnoreCase(trimmed))
                            || (u.getFullName() != null && u.getFullName().toLowerCase().startsWith(trimmed.toLowerCase()))
                            || (u.getId() != null && u.getId().toLowerCase().contains(trimmed.toLowerCase())))
                    .findFirst();
        }

        User user = userOptional.orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + identifier));

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + (user.getRole() != null ? user.getRole() : "USER"));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail() != null ? user.getEmail() : user.getId(),
                user.getPassword(),
                Collections.singletonList(authority)
        );
    }
}
