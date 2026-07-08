package com.harvesthub.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document; // Use this instead of @Entity

@Data
@Document(collection = "users")
public class User {

    @Id
    private String id; // Must be String for MongoDB ObjectId

    private String email;
    private String password;
}