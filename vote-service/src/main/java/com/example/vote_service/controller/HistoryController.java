package com.example.vote_service.controller;

import com.example.vote_service.model.History;
import com.example.vote_service.model.HistoryCategory;
import com.example.vote_service.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * History 컨트롤러
 * - 히스토리 조회 API 제공
 */
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    /**
     * 특정 그룹의 히스토리 조회
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<History>> getGroupHistory(@PathVariable UUID groupId) {
        List<History> history = historyService.getGroupHistory(groupId);
        return ResponseEntity.ok(history);
    }

    /**
     * 특정 그룹의 특정 카테고리 히스토리 조회
     */
    @GetMapping("/group/{groupId}/category/{category}")
    public ResponseEntity<List<History>> getGroupHistoryByCategory(
            @PathVariable UUID groupId,
            @PathVariable HistoryCategory category) {
        List<History> history = historyService.getGroupHistoryByCategory(groupId, category);
        return ResponseEntity.ok(history);
    }
}
