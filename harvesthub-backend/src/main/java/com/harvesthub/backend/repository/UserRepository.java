package com.harvesthub.backend.repository;

import com.harvesthub.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMobileNumber(String mobileNumber);
    Optional<User> findByNic(String nic);
    Optional<User> findByResetPasswordToken(String token);
    java.util.List<User> findByRole(String role);
}
