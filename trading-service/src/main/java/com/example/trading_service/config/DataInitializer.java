package com.example.trading_service.config;

import com.example.trading_service.domain.Stock;
import com.example.trading_service.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final StockRepository stockRepository;

    @Override
    public void run(String... args) throws Exception {
        // 샘플 주식 데이터가 없으면 생성
        if (stockRepository.count() == 0) {
            createSampleStocks();
            log.info("샘플 주식 데이터가 생성되었습니다.");
        }
    }

    private void createSampleStocks() {
        // 시가총액 기준 상위 10개 한국 주식
        createStock("005930", "삼성전자", Stock.Country.KR);
        createStock("000660", "SK하이닉스", Stock.Country.KR);
        createStock("035420", "NAVER", Stock.Country.KR);
        createStock("207940", "삼성바이오로직스", Stock.Country.KR);
        createStock("006400", "삼성SDI", Stock.Country.KR);
        createStock("051910", "LG화학", Stock.Country.KR);
        createStock("035720", "카카오", Stock.Country.KR);
        createStock("068270", "셀트리온", Stock.Country.KR);
        createStock("005380", "현대차", Stock.Country.KR);
        createStock("323410", "카카오뱅크", Stock.Country.KR);

        // TIGER ETF 5개
        createStock("069500", "KODEX 200", Stock.Country.KR);
        createStock("122630", "KODEX 레버리지", Stock.Country.KR);
        createStock("114800", "KODEX 인버스", Stock.Country.KR);
        createStock("091160", "KODEX 반도체", Stock.Country.KR);
        createStock("091170", "KODEX 은행", Stock.Country.KR);
    }

    private void createStock(String stockCode, String stockName, Stock.Country country) {
        Stock stock = new Stock();
        stock.setStockCode(stockCode);
        stock.setStockName(stockName);
        stock.setCountry(country);
        stock.setEnabled(true);
        stockRepository.save(stock);
    }
}


