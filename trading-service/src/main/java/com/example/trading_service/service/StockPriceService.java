package com.example.trading_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockPriceService {

    @Value("${kis.base-url}")
    private String baseUrl;

    @Value("${kis.appkey}")
    private String appKey;

    @Value("${kis.appsecret}")
    private String appSecret;

    private final RestTemplate restTemplate;
    private final KisTokenService kisTokenService;

    public Map<String, Object> getCurrentPrice(String stockCode) {
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price"
                + "?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD=" + stockCode;

        // 유효한 토큰을 자동으로 가져옵니다
        String accessToken = kisTokenService.getValidAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.add("authorization", "Bearer " + accessToken);
        headers.add("appkey", appKey);
        headers.add("appsecret", appSecret);
        headers.add("tr_id", "FHKST01010100");
        headers.add("custtype", "P");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                log.error("시세 조회 실패: HTTP {} - {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("시세 조회 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("시세 조회 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("시세 조회 실패: " + e.getMessage(), e);
        }
    }

    // 주식 상세 정보 조회 (현재가 + 거래량 + 고저가 등)
    public Map<String, Object> getStockDetail(String stockCode) {
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price"
                + "?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD=" + stockCode;

        String accessToken = kisTokenService.getValidAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.add("authorization", "Bearer " + accessToken);
        headers.add("appkey", appKey);
        headers.add("appsecret", appSecret);
        headers.add("tr_id", "FHKST01010100");
        headers.add("custtype", "P");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                log.error("주식 상세 조회 실패: HTTP {} - {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("주식 상세 조회 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("주식 상세 조회 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("주식 상세 조회 실패: " + e.getMessage(), e);
        }
    }

    // 주식 차트 데이터 조회 (일봉 데이터)
    public Map<String, Object> getStockChart(String stockCode, String period) {
        // 현재 날짜 기준으로 최근 1년 데이터 조회
        String endDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String startDate = java.time.LocalDate.now().minusYears(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice"
                + "?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD=" + stockCode
                + "&FID_INPUT_DATE_1=" + startDate + "&FID_INPUT_DATE_2=" + endDate
                + "&FID_PERIOD_DIV_CODE=D&FID_ORG_ADJ_PRC=0";

        String accessToken = kisTokenService.getValidAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.add("authorization", "Bearer " + accessToken);
        headers.add("appkey", appKey);
        headers.add("appsecret", appSecret);
        headers.add("tr_id", "FHKST03010100");
        headers.add("custtype", "P");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                log.error("차트 데이터 조회 실패: HTTP {} - {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("차트 데이터 조회 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("차트 데이터 조회 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("차트 데이터 조회 실패: " + e.getMessage(), e);
        }
    }

    // 주식 호가 데이터 조회
    public Map<String, Object> getOrderBook(String stockCode) {
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-asking-price-exp-ccn"
                + "?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD=" + stockCode;

        String accessToken = kisTokenService.getValidAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.add("authorization", "Bearer " + accessToken);
        headers.add("appkey", appKey);
        headers.add("appsecret", appSecret);
        headers.add("tr_id", "FHKST01010200");
        headers.add("custtype", "P");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                log.error("호가 데이터 조회 실패: HTTP {} - {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("호가 데이터 조회 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("호가 데이터 조회 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("호가 데이터 조회 실패: " + e.getMessage(), e);
        }
    }
}
