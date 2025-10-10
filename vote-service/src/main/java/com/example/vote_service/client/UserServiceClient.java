package com.example.vote_service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * User Service 클라이언트
 * - user-service와 통신하여 사용자 정보 조회
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${user-service.url}")
    private String userServiceUrl;

    /**
     * 사용자 닉네임 조회
     */
    public String getUserNickname(UUID userId) {
        try {
            String url = userServiceUrl + "/users/" + userId + "/nickname";
            String jsonResponse = restTemplate.getForObject(url, String.class);
            
            if (jsonResponse != null) {
                JsonNode jsonNode = objectMapper.readTree(jsonResponse);
                String nickname = jsonNode.get("nickname").asText();
                log.debug("사용자 닉네임 조회 성공: userId={}, nickname={}", userId, nickname);
                return nickname;
            }
            
            return "알 수 없는 사용자";
        } catch (Exception e) {
            log.error("사용자 닉네임 조회 실패: userId={}, error={}", userId, e.getMessage());
            return "알 수 없는 사용자"; // 기본값 반환
        }
    }
}
