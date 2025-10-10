package com.example.user_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "`groups`")
public class Group {

    @Id
    @org.hibernate.annotations.UuidGenerator
    @Column(name = "groupId", columnDefinition = "BINARY(16)")
    private UUID groupId;

    @Column(name = "groupName", nullable = false, length = 100)
    private String groupName;

    @Column(name = "ownerId", columnDefinition = "BINARY(16)", nullable = false)
    private UUID ownerId;

    @Column(name = "goalAmount", nullable = false)
    private Integer goalAmount;

    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    public static Group create(String groupName, UUID ownerId, Integer goalAmount) {
        Group group = new Group();
        group.groupName = groupName;
        group.ownerId = ownerId;
        group.goalAmount = goalAmount;
        return group;
    }
}
