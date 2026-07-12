package com.harvesthub.backend.service;

import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(User user) {
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        // Hash the incoming SHA-256 password with BCrypt
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public Optional<User> findByEmailOrMobile(String emailOrMobile) {
        return userRepository.findByEmail(emailOrMobile)
                .or(() -> userRepository.findByMobileNumber(emailOrMobile));
    }
}
