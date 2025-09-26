package com.example.user_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        return ResponseEntity.ok(Map.of(
            "message", "Get all users",
            "data", "[]",
            "status", "success"
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
            "message", "Get user by id: " + id,
            "data", Map.of("id", id, "name", "Test User"),
            "status", "success"
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> userData) {
        return ResponseEntity.ok(Map.of(
            "message", "User created successfully",
            "data", userData,
            "status", "success"
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> userData) {
        return ResponseEntity.ok(Map.of(
            "message", "User updated successfully",
            "data", Map.of("id", id, "updatedData", userData),
            "status", "success"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
            "message", "User deleted successfully",
            "data", Map.of("id", id),
            "status", "success"
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "user-service",
            "message", "User Service is running"
        ));
    }
}
