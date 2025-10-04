package com.example.user_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Column(name = "userName", nullable = false, length = 50)
    private String userName;

    @Column(name = "nickName", nullable = false, length = 50)
    private String nickName;

    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static User create(String password, String userName, String nickName) {
        User newUser = new User();
        newUser.password = password;
        newUser.userName = userName;
        newUser.nickName = nickName;
        return newUser;
    }

    public void changeNickname(String nickname) {
        this.nickName = nickname;
    }
}
