package com.example.vote_service.event;

import com.example.vote_service.model.History;
import lombok.Getter;

/**
 * 히스토리 생성 이벤트
 * - DB에 History 엔티티가 저장될 때 발행되는 이벤트
 */
@Getter
public class HistoryCreatedEvent {
    
    private final History history;
    
    public HistoryCreatedEvent(History history) {
        this.history = history;
    }
}
