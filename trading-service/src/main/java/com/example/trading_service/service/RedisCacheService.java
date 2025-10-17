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
    private static final String USER_BALANCE_KEY = "user:balance:";
    private static final String USER_HOLDINGS_KEY = "user:holdings:";
    private static final String KIS_TOKEN_KEY = "kis:token";
    
    // 캐시 TTL (Time To Live)
    private static final Duration STOCK_PRICE_TTL = Duration.ofMinutes(1); // 1분
    private static final Duration USER_BALANCE_TTL = Duration.ofMinutes(5); // 5분
    private static final Duration USER_HOLDINGS_TTL = Duration.ofMinutes(10); // 10분
    private static final Duration KIS_TOKEN_TTL = Duration.ofHours(23); // 23시간

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




