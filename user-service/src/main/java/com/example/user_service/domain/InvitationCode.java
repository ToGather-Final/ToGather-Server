package com.example.user_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "invitation_code")
public class InvitationCode {

    @Id
    @Column(name = "code", length = 4, nullable = false, unique = true)
    private String code;

    @Column(name = "groupId", columnDefinition = "BINARY(16)", nullable = false)
    private UUID groupId;

    @Column(name = "isExpired", nullable = false)
    private Boolean isExpired;

    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isExpired == null) {
             this.isExpired = false;
        }
    }

    public static InvitationCode issue(UUID groupId) {
        InvitationCode invitationCode = new InvitationCode();
        invitationCode.code = generateCode();
        invitationCode.groupId = groupId;
        invitationCode.isExpired = false;
        return invitationCode;
    }

    public boolean isExpired() {
        return Boolean.TRUE.equals(this.isExpired);
    }

    public void expire() {
        this.isExpired = true;
    }

    public static String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
}

