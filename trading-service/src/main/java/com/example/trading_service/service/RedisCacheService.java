package com.example.trading_service.service;

import com.example.trading_service.domain.Stock;
import com.example.trading_service.dto.StockPriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    // 캐시 키 상수
    private static final String STOCK_PRICE_KEY = "stock:price:";
    private static final String STOCK_PREV_CLOSE_KEY = "stock:prevclose:";
    private static final String USER_BALANCE_KEY = "user:balance:";
    private static final String USER_HOLDINGS_KEY = "user:holdings:";
    private static final String KIS_TOKEN_KEY = "kis:token";
    private static final String WEBSOCKET_ORDERBOOK_KEY = "websocket:orderbook:";
    
    // 캐시 TTL (Time To Live) - 최적화된 값
    private static final Duration STOCK_PRICE_TTL = Duration.ofMinutes(5); // 5분 (웹소켓 연결 안정화)
    private static final Duration USER_BALANCE_TTL = Duration.ofMinutes(5); // 5분
    private static final Duration USER_HOLDINGS_TTL = Duration.ofMinutes(10); // 10분
    private static final Duration KIS_TOKEN_TTL = Duration.ofHours(23); // 23시간
    private static final Duration WEBSOCKET_ORDERBOOK_TTL = Duration.ofMinutes(2); // 2분 (웹소켓 연결 안정화)

    /**
     * 주식 가격 캐싱
     */
    public void cacheStockPrice(UUID stockId, StockPriceResponse priceResponse) {
        String key = STOCK_PRICE_KEY + stockId;
        try {
            redisTemplate.opsForValue().set(key, priceResponse, STOCK_PRICE_TTL);
            log.debug("주식 가격 캐싱 완료 - 주식ID: {}, 가격: {}", stockId, priceResponse.getCurrentPrice());
        } catch (Exception e) {
            log.error("주식 가격 캐싱 실패 - 주식ID: {}", stockId, e);
        }
    }

    /**
     * 주식 가격 캐싱 (커스텀 TTL)
     */
    public void cacheStockPriceWithTTL(UUID stockId, StockPriceResponse priceResponse, Duration customTTL) {
        String key = STOCK_PRICE_KEY + stockId;
        try {
            redisTemplate.opsForValue().set(key, priceResponse, customTTL);
            log.debug("주식 가격 캐싱 완료 (커스텀 TTL: {}) - 주식ID: {}, 가격: {}", customTTL, stockId, priceResponse.getCurrentPrice());
        } catch (Exception e) {
            log.error("주식 가격 캐싱 실패 - 주식ID: {}", stockId, e);
        }
    }

    /**
     * 주식 가격 조회
     */
    public StockPriceResponse getCachedStockPrice(UUID stockId) {
        String key = STOCK_PRICE_KEY + stockId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof StockPriceResponse) {
                log.debug("주식 가격 캐시 히트 - 주식ID: {}", stockId);
                return (StockPriceResponse) cached;
            }
        } catch (Exception e) {
            log.error("주식 가격 캐시 조회 실패 - 주식ID: {}", stockId, e);
        }
        return null;
    }

    /**
     * 사용자 잔고 캐싱
     */
    public void cacheUserBalance(UUID userId, BigDecimal balance) {
        String key = USER_BALANCE_KEY + userId;
        try {
            redisTemplate.opsForValue().set(key, balance, USER_BALANCE_TTL);
            log.debug("사용자 잔고 캐싱 완료 - 사용자ID: {}, 잔고: {}", userId, balance);
        } catch (Exception e) {
            log.error("사용자 잔고 캐싱 실패 - 사용자ID: {}", userId, e);
        }
    }

    /**
     * 사용자 잔고 조회
     */
    public BigDecimal getCachedUserBalance(UUID userId) {
        String key = USER_BALANCE_KEY + userId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof BigDecimal) {
                log.debug("사용자 잔고 캐시 히트 - 사용자ID: {}", userId);
                return (BigDecimal) cached;
            }
        } catch (Exception e) {
            log.error("사용자 잔고 캐시 조회 실패 - 사용자ID: {}", userId, e);
        }
        return null;
    }

    /**
     * 사용자 잔고 캐시 삭제
     */
    public void evictUserBalance(UUID userId) {
        String key = USER_BALANCE_KEY + userId;
        try {
            redisTemplate.delete(key);
            log.debug("사용자 잔고 캐시 삭제 - 사용자ID: {}", userId);
        } catch (Exception e) {
            log.error("사용자 잔고 캐시 삭제 실패 - 사용자ID: {}", userId, e);
        }
    }

    /**
     * 사용자 보유 주식 캐싱
     */
    public void cacheUserHoldings(UUID userId, Object holdings) {
        String key = USER_HOLDINGS_KEY + userId;
        try {
            redisTemplate.opsForValue().set(key, holdings, USER_HOLDINGS_TTL);
            log.debug("사용자 보유 주식 캐싱 완료 - 사용자ID: {}", userId);
        } catch (Exception e) {
            log.error("사용자 보유 주식 캐싱 실패 - 사용자ID: {}", userId, e);
        }
    }

    /**
     * 사용자 보유 주식 조회
     */
    public Object getCachedUserHoldings(UUID userId) {
        String key = USER_HOLDINGS_KEY + userId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("사용자 보유 주식 캐시 히트 - 사용자ID: {}", userId);
                return cached;
            }
        } catch (Exception e) {
            log.error("사용자 보유 주식 캐시 조회 실패 - 사용자ID: {}", userId, e);
        }
        return null;
    }

    /**
     * 사용자 보유 주식 캐시 삭제
     */
    public void evictUserHoldings(UUID userId) {
        String key = USER_HOLDINGS_KEY + userId;
        try {
            redisTemplate.delete(key);
            log.debug("사용자 보유 주식 캐시 삭제 - 사용자ID: {}", userId);
        } catch (Exception e) {
            log.error("사용자 보유 주식 캐시 삭제 실패 - 사용자ID: {}", userId, e);
        }
    }

    /**
     * KIS 토큰 캐싱
     */
    public void cacheKisToken(String token) {
        try {
            redisTemplate.opsForValue().set(KIS_TOKEN_KEY, token, KIS_TOKEN_TTL);
            log.debug("KIS 토큰 캐싱 완료");
        } catch (Exception e) {
            log.error("KIS 토큰 캐싱 실패", e);
        }
    }

    /**
     * KIS 토큰 조회
     */
    public String getCachedKisToken() {
        try {
            Object cached = redisTemplate.opsForValue().get(KIS_TOKEN_KEY);
            if (cached instanceof String) {
                log.debug("KIS 토큰 캐시 히트");
                return (String) cached;
            }
        } catch (Exception e) {
            log.error("KIS 토큰 캐시 조회 실패", e);
        }
        return null;
    }

    /**
     * KIS 토큰 캐시 삭제
     */
    public void evictKisToken() {
        try {
            redisTemplate.delete(KIS_TOKEN_KEY);
            log.debug("KIS 토큰 캐시 삭제");
        } catch (Exception e) {
            log.error("KIS 토큰 캐시 삭제 실패", e);
        }
    }

    /**
     * 특정 패턴의 캐시 삭제
     */
    public void evictCacheByPattern(String pattern) {
        try {
            redisTemplate.delete(redisTemplate.keys(pattern));
            log.debug("패턴 캐시 삭제 완료 - 패턴: {}", pattern);
        } catch (Exception e) {
            log.error("패턴 캐시 삭제 실패 - 패턴: {}", pattern, e);
        }
    }

    /**
     * WebSocket 호가 데이터 캐싱
     */
    public void cacheWebSocketOrderBook(String stockCode, Object orderBookData) {
        String key = WEBSOCKET_ORDERBOOK_KEY + stockCode;
        try {
            // Redis 연결 상태 확인
            if (redisTemplate.getConnectionFactory() == null) {
                log.warn("⚠️ Redis 연결 팩토리가 null - 캐싱 건너뜀: {}", stockCode);
                return;
            }
            
            // 기존 캐시 삭제 (직렬화 문제 해결을 위해)
            redisTemplate.delete(key);
            
            redisTemplate.opsForValue().set(key, orderBookData, WEBSOCKET_ORDERBOOK_TTL);
            log.info("✅ WebSocket 호가 데이터 캐싱 완료 - 종목코드: {} (JSON 직렬화)", stockCode);
            
            // 캐싱 검증: 바로 조회해서 확인 (오류 발생해도 계속 진행)
            try {
                Object cached = redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    log.info("🔍 캐싱 검증 성공 - 종목코드: {}, 타입: {}", stockCode, cached.getClass().getSimpleName());
                } else {
                    log.warn("⚠️ 캐싱 검증 실패 - 종목코드: {}", stockCode);
                }
            } catch (Exception validationError) {
                log.warn("⚠️ 캐싱 검증 중 오류 발생 - 종목코드: {} - {}", stockCode, validationError.getMessage());
                // 검증 오류는 무시하고 계속 진행
            }
        } catch (IllegalStateException e) {
            // Redis 연결이 종료된 상태일 때 (애플리케이션 종료 중)
            if (e.getMessage() != null && 
                (e.getMessage().contains("STOPPING") || 
                 e.getMessage().contains("destroyed") || 
                 e.getMessage().contains("cannot be used anymore"))) {
                log.debug("Redis 연결 종료 상태 - 캐싱 건너뜀: {}", stockCode);
            } else {
                log.warn("⚠️ Redis 연결 상태 오류 - 종목코드: {} - {}", stockCode, e.getMessage());
            }
        } catch (Exception e) {
            log.error("❌ WebSocket 호가 데이터 캐싱 실패 - 종목코드: {} - {}", stockCode, e.getMessage(), e);
            log.error("🔍 캐싱 실패 상세 정보 - 데이터 타입: {}, 데이터 내용: {}", 
                    orderBookData != null ? orderBookData.getClass().getSimpleName() : "null",
                    orderBookData);
            // Redis 연결 문제 시 캐싱을 건너뛰고 계속 진행
        }
    }

    /**
     * WebSocket 호가 데이터 조회
     */
    public Object getCachedWebSocketOrderBook(String stockCode) {
        String key = WEBSOCKET_ORDERBOOK_KEY + stockCode;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.info("🔍 WebSocket 호가 데이터 캐시 히트 - 종목코드: {}, 타입: {}", stockCode, cached.getClass().getSimpleName());
                return cached;
            } else {
                log.warn("⚠️ WebSocket 호가 데이터 캐시 미스 - 종목코드: {} (캐시 없음)", stockCode);
            }
        } catch (Exception e) {
            log.error("❌ WebSocket 호가 데이터 캐시 조회 실패 - 종목코드: {}", stockCode, e);
        }
        return null;
    }

    /**
     * WebSocket 호가 데이터 캐시 삭제
     */
    public void evictWebSocketOrderBook(String stockCode) {
        String key = WEBSOCKET_ORDERBOOK_KEY + stockCode;
        try {
            redisTemplate.delete(key);
            log.debug("WebSocket 호가 데이터 캐시 삭제 - 종목코드: {}", stockCode);
        } catch (Exception e) {
            log.error("WebSocket 호가 데이터 캐시 삭제 실패 - 종목코드: {}", stockCode, e);
        }
    }

    /**
     * 모든 WebSocket 호가 데이터 캐시 삭제
     */
    public void evictAllWebSocketOrderBooks() {
        try {
            redisTemplate.delete(redisTemplate.keys(WEBSOCKET_ORDERBOOK_KEY + "*"));
            log.debug("모든 WebSocket 호가 데이터 캐시 삭제 완료");
        } catch (Exception e) {
            log.error("모든 WebSocket 호가 데이터 캐시 삭제 실패", e);
        }
    }

    /**
     * Redis 키 패턴으로 조회 (WebSocketOrderBookService에서 사용)
     */
    public java.util.Set<String> getKeysByPattern(String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            log.error("Redis 키 패턴 조회 실패 - 패턴: {}", pattern, e);
            return java.util.Set.of();
        }
    }

    /**
     * 전일 종가 캐싱 (stockCode 기반)
     */
    public void cachePrevClosePrice(String stockCode, Float prevClosePrice) {
        String key = STOCK_PREV_CLOSE_KEY + stockCode;
        try {
            // 하루 동안 캐싱 (전일 종가는 하루 종일 변하지 않음)
            redisTemplate.opsForValue().set(key, prevClosePrice, Duration.ofHours(24));
            log.debug("전일 종가 캐싱 완료 - 종목코드: {}, 전일종가: {}", stockCode, prevClosePrice);
        } catch (Exception e) {
            log.error("전일 종가 캐싱 실패 - 종목코드: {}", stockCode, e);
        }
    }

    /**
     * 전일 종가 조회 (stockCode 기반)
     */
    public Float getCachedPrevClosePrice(String stockCode) {
        String key = STOCK_PREV_CLOSE_KEY + stockCode;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof Float) {
                log.debug("전일 종가 캐시 히트 - 종목코드: {}", stockCode);
                return (Float) cached;
            } else if (cached instanceof Double) {
                return ((Double) cached).floatValue();
            } else if (cached instanceof Number) {
                return ((Number) cached).floatValue();
            }
        } catch (Exception e) {
            log.debug("전일 종가 캐시 조회 실패 - 종목코드: {}", stockCode, e);
        }
        return null;
    }

    /**
     * 토큰 만료 시 관련 캐시 무효화
     */
    public void invalidateTokenRelatedCache() {
        try {
            // KIS 토큰 캐시 삭제
            evictKisToken();
            
            // WebSocket 호가 데이터 캐시 삭제 (토큰 만료로 인한 재연결 필요)
            evictAllWebSocketOrderBooks();
            
            log.info("🔄 토큰 만료로 인한 관련 캐시 무효화 완료");
        } catch (Exception e) {
            log.error("토큰 관련 캐시 무효화 실패", e);
        }
    }
    
    /**
     * 모든 캐시 삭제 (개발/테스트용)
     */
    public void clearAllCache() {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
            log.warn("모든 Redis 캐시 삭제 완료");
        } catch (Exception e) {
            log.error("모든 Redis 캐시 삭제 실패", e);
        }
    }
}




