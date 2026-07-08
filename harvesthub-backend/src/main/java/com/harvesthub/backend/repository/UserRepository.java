package com.harvesthub.backend.repository;

import com.harvesthub.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    // Spring Boot automatically handles standard queries like findById or deleteById
    Optional<User> findByEmail(String email);
}