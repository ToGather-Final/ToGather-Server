package com.example.trading_service.service;

import com.example.trading_service.dto.StockPriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private final RedisCacheService redisCacheService;

    /**
     * 캐싱이 적용된 주식 가격 조회 (UUID 기반) - Redis 캐시 우선
     */
    public StockPriceResponse getCachedStockPrice(UUID stockId, String stockCode) {
        // 1. Redis 캐시에서 조회
        StockPriceResponse cachedPrice = redisCacheService.getCachedStockPrice(stockId);
        if (cachedPrice != null) {
            log.info("🚀 Redis 캐시에서 주식 가격 반환: {}", stockCode);
            return cachedPrice;
        }

        // 2. 캐시에 없으면 API 호출
        log.info("📡 API 호출로 주식 가격 조회: {}", stockCode);
        Map<String, Object> apiResponse = getCurrentPrice(stockCode);
        
        // 3. API 응답을 StockPriceResponse로 변환
        StockPriceResponse priceResponse = convertToStockPriceResponse(apiResponse);
        
        // 4. Redis에 캐싱
        redisCacheService.cacheStockPrice(stockId, priceResponse);
        
        return priceResponse;
    }


    /**
     * API 응답을 StockPriceResponse로 변환
     */
    private StockPriceResponse convertToStockPriceResponse(Map<String, Object> apiResponse) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> output = (Map<String, Object>) apiResponse.get("output");
            
            if (output == null) {
                throw new RuntimeException("API 응답에서 output 데이터를 찾을 수 없습니다.");
            }

            StockPriceResponse response = new StockPriceResponse();
            response.setCurrentPrice(new BigDecimal(output.get("stck_prpr").toString()));
            response.setChangePrice(new BigDecimal(output.get("prdy_vrss").toString()));
            float changeRate = Float.parseFloat(output.get("prdy_ctrt").toString().replace(",", ""));
            response.setChangeRate(Math.round(changeRate * 100.0f) / 100.0f);
            response.setVolume(Long.parseLong(output.get("acml_vol").toString()));
            response.setHighPrice(new BigDecimal(output.get("stck_hgpr").toString()));
            response.setLowPrice(new BigDecimal(output.get("stck_lwpr").toString()));
            response.setOpenPrice(new BigDecimal(output.get("stck_oprc").toString()));
            response.setPrevClosePrice(new BigDecimal(output.get("stck_sdpr").toString()));
            
            return response;
        } catch (Exception e) {
            log.error("API 응답 변환 실패: {}", e.getMessage());
            throw new RuntimeException("주식 가격 데이터 변환 실패", e);
        }
    }

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

    // 주식 차트 데이터 조회 (기간별 데이터)
    public Map<String, Object> getStockChart(String stockCode, String period) {
        // 기간별 조회 기간 설정
        String endDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String startDate;
        
        // 기간별 조회 기간 설정
        switch (period.toUpperCase()) {
            case "D": // 일봉: 최근 1년
                startDate = java.time.LocalDate.now().minusYears(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                break;
            case "W": // 주봉: 최근 2년
                startDate = java.time.LocalDate.now().minusYears(2).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                break;
            case "M": // 월봉: 최근 5년
                startDate = java.time.LocalDate.now().minusYears(5).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                break;
            case "Y": // 연봉: 최근 10년
                startDate = java.time.LocalDate.now().minusYears(10).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                break;
            default: // 기본값: 일봉
                startDate = java.time.LocalDate.now().minusYears(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                break;
        }
        
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice"
                + "?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD=" + stockCode
                + "&FID_INPUT_DATE_1=" + startDate + "&FID_INPUT_DATE_2=" + endDate
                + "&FID_PERIOD_DIV_CODE=" + period.toUpperCase() + "&FID_ORG_ADJ_PRC=0";

        log.info("차트 데이터 요청 - 종목코드: {}, 기간: {}, 조회기간: {} ~ {}", 
                stockCode, period.toUpperCase(), startDate, endDate);

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
