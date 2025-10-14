package com.example.vote_service.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * History 엔티티
 * - 그룹 활동 히스토리를 기록
 * - 투표, 매매, 예수금, 페이, 목표 달성 등의 활동 추적
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "history")
public class History {

    @Id
    @org.hibernate.annotations.UuidGenerator
    @Column(name = "history_id", columnDefinition = "BINARY(16)")
    private UUID historyId;

    @Column(name = "group_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID groupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "history_category", nullable = false, length = 20)
    private HistoryCategory historyCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "history_type", nullable = false, length = 30)
    private HistoryType historyType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "date", nullable = false, length = 20)
    private String date;

    @Column(name = "payload", columnDefinition = "JSON")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 정적 팩토리 메서드 - History 생성
     */
    public static History create(UUID groupId, HistoryCategory category, HistoryType type, 
                                String title, String payload) {
        History history = new History();
        history.groupId = groupId;
        history.historyCategory = category;
        history.historyType = type;
        history.title = title;
        history.date = LocalDateTime.now().toString().substring(0, 19); // YYYY-MM-DD HH:mm:ss
        history.payload = payload;
        return history;
    }

    /**
     * 테스트용 메서드 - historyId 설정
     * 실제 운영에서는 JPA가 자동으로 생성하므로 사용하지 않음
     */
    public void setHistoryIdForTest(UUID historyId) {
        this.historyId = historyId;
    }
}
