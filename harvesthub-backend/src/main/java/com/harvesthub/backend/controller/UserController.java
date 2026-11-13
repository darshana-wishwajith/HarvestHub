package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.UserDto;
import com.harvesthub.backend.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin-only (enforced in SecurityConfig). Returns DTOs, never password hashes. */
@RestController
@RequestMapping({"/api/users", "/users"})
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDto> getUsers() {
        return userService.getAllUsers().stream().map(UserDto::from).toList();
    }
}
