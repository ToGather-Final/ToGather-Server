package com.example.vote_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/vote")
public class VoteController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllVotes() {
        return ResponseEntity.ok(Map.of(
            "message", "Get all votes",
            "data", "[]",
            "status", "success"
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getVoteById(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
            "message", "Get vote by id: " + id,
            "data", Map.of("id", id, "option", "Option A"),
            "status", "success"
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createVote(@RequestBody Map<String, Object> voteData) {
        return ResponseEntity.ok(Map.of(
            "message", "Vote created successfully",
            "data", voteData,
            "status", "success"
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "vote-service",
            "message", "Vote Service is running"
        ));
    }
}
