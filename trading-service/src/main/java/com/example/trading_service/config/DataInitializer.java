package com.example.trading_service.config;

import com.example.trading_service.domain.Stock;
import com.example.trading_service.repository.StockRepository;
import com.example.trading_service.service.KisWebSocketClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final StockRepository stockRepository;
    private final KisWebSocketClient kisWebSocketClient;

    // ETF 코드와 이름 매핑
    private static final Map<String, String> ETF_CODES = Map.ofEntries(
        // SOL ETF
        Map.entry("446720", "SOL 미국배당다우존스"),
        Map.entry("466920", "SOL 조선TOP3플러스"),
        Map.entry("0105E0", "SOL 코리아고배당"),
        Map.entry("497880", "SOL CD금리&머니마켓액티브"),
        // KODEX ETF
        Map.entry("069500", "KODEX 200"),
        Map.entry("379800", "KODEX 미국S&P500TR"),
        // TIGER ETF
        Map.entry("133690", "TIGER 미국나스닥100"),
        Map.entry("381170", "TIGER 미국테크TOP10 INDXX")
    );

    @Override
    public void run(String... args) throws Exception {
        long stockCount = stockRepository.count();
        log.info("📊 현재 데이터베이스에 저장된 주식 수: {}", stockCount);
        
        if (stockCount == 0) {
            log.info("🚀 주식/ETF 데이터 생성 시작...");
            createAllStocks();
            log.info("✅ 주식/ETF 데이터 생성 완료. 총 {}개 생성됨", stockRepository.count());
        } else {
            log.info("📋 기존 주식 데이터가 존재합니다.");
        }
        
        startWebSocketConnection();
    }

    private void startWebSocketConnection() {
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                log.info("🔗 KisWebSocket 연결 시작...");
                kisWebSocketClient.connect();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("WebSocket 연결 대기 중 스레드 인터럽트 발생", e);
            }
        }).start();
    }
    
    /**
     * 장외 시간인지 확인 (주말, 공휴일, 장외 시간)
     */
    private boolean isMarketClosed() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.DayOfWeek dayOfWeek = now.getDayOfWeek();
        
        // 주말 체크 (토요일, 일요일)
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return true;
        }
        
        // 장외 시간 체크 (09:00 ~ 15:30 외)
        int hour = now.getHour();
        int minute = now.getMinute();
        int currentTime = hour * 100 + minute;
        
        // 09:00 ~ 15:30 외의 시간
        if (currentTime < 900 || currentTime > 1530) {
            return true;
        }
        
        return false;
    }

    private void createAllStocks() {
        // 국내 주식 10개 생성
        createStocks();
        
        // ETF 6개 생성
        ETF_CODES.forEach((code, name) -> 
            createStock(code, name, Stock.Country.KR, Stock.Market.KOSPI, "500"));
    }

    private void createMissingStocks() {
        // 국내 주식 10개 확인 및 생성
        createMissingStocksFromMap(getStockMap());
        
        // ETF 6개 확인 및 생성
        createMissingStocksFromMap(ETF_CODES);
    }

    private Map<String, String> getStockMap() {
        return Map.ofEntries(
                Map.entry("005930", "삼성전자"),
                Map.entry("000660", "SK하이닉스"),
                Map.entry("373220", "LG에너지솔루션"),
                Map.entry("000150", "두산에너빌리티"),
                Map.entry("005380", "현대차"),
                Map.entry("012450", "한화에어로스페이스"),
                Map.entry("329180", "HD현대중공업"),
                Map.entry("000270", "기아"),
                Map.entry("105560", "KB금융"),
                Map.entry("068270", "셀트리온")
        );
    }

    private void createMissingStocksFromMap(Map<String, String> stockMap) {
        stockMap.forEach((code, name) -> {
            // 이미 존재하는지 확인
            if (!stockRepository.existsByStockCode(code)) {
                String prdtTypeCd = stockMap == ETF_CODES ? "500" : "300";
                createStock(code, name, Stock.Country.KR, Stock.Market.KOSPI, prdtTypeCd);
            } else {
                log.debug("주식 코드 {}는 이미 존재합니다.", code);
            }
        });
    }

    private void createStock(String stockCode, String stockName, Stock.Country country, Stock.Market market, String prdtTypeCd) {
        Stock stock = new Stock();
        stock.setStockCode(stockCode);
        stock.setStockName(stockName);
        stock.setCountry(country);
        stock.setMarket(market);
        stock.setPrdtTypeCd(prdtTypeCd);
        stock.setEnabled(true);
        
        // 이미지 경로 설정
        String imagePath = getImagePath(stockCode, stockName, prdtTypeCd);
        stock.setStockImage(imagePath);
        
        stockRepository.save(stock);
        log.info("✅ 주식 데이터 생성 완료: {} - {} (타입: {}, 이미지: {})", stockCode, stockName, prdtTypeCd, imagePath);
    }

    private void createStocks() {
        getStockMap().forEach((code, name) ->
                createStock(code, name, Stock.Country.KR, Stock.Market.KOSPI, "300"));
    }

    /**
     * 주식 코드와 이름에 따라 적절한 이미지 경로를 반환
     */
    private String getImagePath(String stockCode, String stockName, String prdtTypeCd) {
        // 삼성 관련 주식들 (삼성전자, 삼성바이오로직스, 삼성물산)
        if (stockName.contains("삼성")) {
            return "/images/stock/samsung.png";
        }
        
        // ETF인 경우
        if ("500".equals(prdtTypeCd)) {
            if (stockName.startsWith("SOL")) {
                return "/images/stock/soletf.png";
            } else if (stockName.startsWith("KODEX")) {
                return "/images/stock/kodexetf.png";
            } else if (stockName.startsWith("TIGER")) {
                return "/images/stock/tigeretf.png";
            } else if (stockName.startsWith("KBSTAR")) {
                return "/images/stock/kbstart.png";
            }
        }
        
        // 나머지 주식들은 코드명으로 이미지 파일명 설정
        return "/images/stock/" + stockCode + ".png";
    }
}
