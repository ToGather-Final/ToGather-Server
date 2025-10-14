package com.example.module_common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class VoteEvent implements Serializable {
    private String eventType;
    private Long voteId;
    private Long userId;
    private Long tradingId;
    private String voteType;
    private String status;
    private LocalDateTime timestamp;

    public VoteEvent() {}

    public VoteEvent(String eventType, Long voteId, Long userId, Long tradingId, String voteType, String status) {
        this.eventType = eventType;
        this.voteId = voteId;
        this.userId = userId;
        this.tradingId = tradingId;
        this.voteType = voteType;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getVoteId() {
        return voteId;
    }

    public void setVoteId(Long voteId) {
        this.voteId = voteId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTradingId() {
        return tradingId;
    }

    public void setTradingId(Long tradingId) {
        this.tradingId = tradingId;
    }

    public String getVoteType() {
        return voteType;
    }

    public void setVoteType(String voteType) {
        this.voteType = voteType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "VoteEvent{" +
                "eventType='" + eventType + '\'' +
                ", voteId=" + voteId +
                ", userId=" + userId +
                ", tradingId=" + tradingId +
                ", voteType='" + voteType + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
