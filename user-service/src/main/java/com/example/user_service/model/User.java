package com.example.user_service.model;

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
@Table(name = "users")
public class User {
    @Id
    @org.hibernate.annotations.UuidGenerator
    @Column(name = "userId", columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "password", nullable = false, length = 60)
    private String password;

    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static User create(String password, String userName, String nickName) {
        User newUser = new User();
        newUser.password = password;
        newUser.username = userName;
        newUser.nickname = nickName;
        return newUser;
    }

    public void changeNickname(String newNickname) {
        if (newNickname == null || newNickname.isBlank()) {
            throw new IllegalArgumentException("닉네임을 입력하세요.");
        }
        this.nickname = newNickname;
    }
}
