package com.example.trading_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class KisTokenService {

    @Value("${kis.base-url}")
    private String baseUrl;

    @Value("${kis.appkey}")
    private String appKey;

    @Value("${kis.appsecret}")
    private String appSecret;

    private final RestTemplate restTemplate;
    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_KEY = "kis:access_token";
    private static final String TOKEN_EXPIRY_KEY = "kis:token_expiry";

    /**
     * 유효한 액세스 토큰을 반환합니다.
     * 토큰이 없거나 만료된 경우 새로 발급받습니다.
     */
    public String getValidAccessToken() {
        String cachedToken = redisTemplate.opsForValue().get(TOKEN_KEY);
        String expiryTime = redisTemplate.opsForValue().get(TOKEN_EXPIRY_KEY);

        if (cachedToken != null && expiryTime != null) {
            LocalDateTime expiry = LocalDateTime.parse(expiryTime);
            if (LocalDateTime.now().isBefore(expiry.minusMinutes(5))) { // 5분 여유를 두고 갱신
                log.debug("캐시된 토큰 사용: {}", cachedToken.substring(0, 20) + "...");
                return cachedToken;
            }
        }

        log.info("토큰이 만료되었거나 없습니다. 새로 발급받습니다.");
        return refreshAccessToken();
    }

    /**
     * KIS API에서 새로운 액세스 토큰을 발급받습니다.
     */
    public String refreshAccessToken() {
        try {
            String url = baseUrl + "/oauth2/tokenP";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 요청 본문 구성
            String requestBody = String.format(
                "{\"grant_type\":\"client_credentials\",\"appkey\":\"%s\",\"appsecret\":\"%s\"}",
                appKey, appSecret
            );

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String accessToken = (String) responseBody.get("access_token");
                Integer expiresIn = (Integer) responseBody.get("expires_in");

                if (accessToken != null && expiresIn != null) {
                    // 토큰을 Redis에 캐시 (만료 시간보다 5분 짧게 설정)
                    LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(expiresIn - 300);
                    
                    redisTemplate.opsForValue().set(TOKEN_KEY, accessToken, expiresIn - 300, TimeUnit.SECONDS);
                    redisTemplate.opsForValue().set(TOKEN_EXPIRY_KEY, expiryTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), expiresIn - 300, TimeUnit.SECONDS);

                    log.info("새로운 토큰 발급 성공: {}... (만료: {})", 
                        accessToken.substring(0, 20), expiryTime);
                    
                    return accessToken;
                } else {
                    log.error("토큰 발급 응답에서 필수 필드가 누락되었습니다: {}", responseBody);
                    throw new RuntimeException("토큰 발급 실패: 응답 형식 오류");
                }
            } else {
                log.error("토큰 발급 실패: HTTP {} - {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("토큰 발급 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("토큰 발급 중 오류 발생", e);
            throw new RuntimeException("토큰 발급 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 토큰 캐시를 무효화합니다.
     */
    public void invalidateToken() {
        redisTemplate.delete(TOKEN_KEY);
        redisTemplate.delete(TOKEN_EXPIRY_KEY);
        log.info("토큰 캐시가 무효화되었습니다.");
    }

    /**
     * 현재 토큰 상태를 확인합니다.
     */
    public Map<String, Object> getTokenStatus() {
        String token = redisTemplate.opsForValue().get(TOKEN_KEY);
        String expiry = redisTemplate.opsForValue().get(TOKEN_EXPIRY_KEY);
        
        return Map.of(
            "hasToken", token != null,
            "tokenPreview", token != null ? token.substring(0, 20) + "..." : "없음",
            "expiryTime", expiry != null ? expiry : "없음",
            "isExpired", expiry != null ? LocalDateTime.now().isAfter(LocalDateTime.parse(expiry)) : true
        );
    }
}
