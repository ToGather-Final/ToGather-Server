package com.example.trading_service.config;

import com.example.trading_service.domain.Stock;
import com.example.trading_service.repository.StockRepository;
import com.example.trading_service.service.KisWebSocketClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final StockRepository stockRepository;
    private final KisWebSocketClient kisWebSocketClient;

    @Override
    public void run(String... args) throws Exception {
        // 샘플 주식 데이터가 없으면 생성
        if (stockRepository.count() == 0) {
            createSampleStocks();
            log.info("샘플 주식 데이터가 생성되었습니다.");
        }
        
        // WebSocket 연결 시작 (3초 후)
        new Thread(() -> {
            try {
                Thread.sleep(3000); // 3초 대기
                log.info("🔗 KisWebSocket 연결 시작...");
                kisWebSocketClient.connect();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("WebSocket 연결 대기 중 스레드 인터럽트 발생", e);
            }
        }).start();
    }

    private void createSampleStocks() {
        // 국내 주식 10개 (KOSPI 대형주)
        createStock("005930", "삼성전자", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("000660", "SK하이닉스", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("035420", "NAVER", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("207940", "삼성바이오로직스", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("006400", "삼성SDI", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("051910", "LG화학", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("035720", "카카오", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("068270", "셀트리온", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("005380", "현대차", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("323410", "카카오뱅크", Stock.Country.KR, Stock.Market.KOSPI);

        // SOL ETF 대표 10종 (국내 + 해외) - 2025.01 업데이트
        createStock("295040", "SOL 200TR", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("466920", "SOL 조선TOP3플러스", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("0105E0", "SOL 코리아고배당", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("497880", "SOL CD금리&머니마켓액티브", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("433330", "SOL 미국S&P500", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("476030", "SOL 미국나스닥100", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("481190", "SOL 미국테크TOP10", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("481180", "SOL 미국AI소프트웨어", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("479620", "SOL 미국AI반도체칩메이커", Stock.Country.KR, Stock.Market.KOSPI);
        createStock("446720", "SOL 미국배당다우존스", Stock.Country.KR, Stock.Market.KOSPI);
    }

    private void createStock(String stockCode, String stockName, Stock.Country country, Stock.Market market) {
        Stock stock = new Stock();
        stock.setStockCode(stockCode);
        stock.setStockName(stockName);
        stock.setCountry(country);
        stock.setMarket(market);
        stock.setEnabled(true);
        stockRepository.save(stock);
    }
}


