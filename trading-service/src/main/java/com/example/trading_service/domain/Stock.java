package com.example.trading_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "stock")
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "stock_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "stock_code", nullable = false, unique = true)
    private String stockCode; // 종목 코드

    @Column(name = "stock_name", nullable = false)
    private String stockName; // 종목 이름

    @Column(name = "stock_image")
    private String stockImage; // 썸네일/로고 이미지 URL

    @Column(name = "country", nullable = false)
    @Enumerated(EnumType.STRING)
    private Country country; // 국가

    @Column(name = "enabled", nullable = false)
    private boolean enabled; // 사용 가능 여부

    public enum Country {
        KR, US
        // 필요시 추가
    }
}
