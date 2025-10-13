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
        // 한국 주식
        Stock samsung = new Stock();
        samsung.setStockCode("005930");
        samsung.setStockName("삼성전자");
        samsung.setCountry(Stock.Country.KR);
        samsung.setEnabled(true);
        stockRepository.save(samsung);

        Stock lgChem = new Stock();
        lgChem.setStockCode("051910");
        lgChem.setStockName("LG화학");
        lgChem.setCountry(Stock.Country.KR);
        lgChem.setEnabled(true);
        stockRepository.save(lgChem);

        Stock naver = new Stock();
        naver.setStockCode("035420");
        naver.setStockName("NAVER");
        naver.setCountry(Stock.Country.KR);
        naver.setEnabled(true);
        stockRepository.save(naver);

        // 미국 주식
        Stock apple = new Stock();
        apple.setStockCode("AAPL");
        apple.setStockName("Apple Inc.");
        apple.setCountry(Stock.Country.US);
        apple.setEnabled(true);
        stockRepository.save(apple);

        Stock tesla = new Stock();
        tesla.setStockCode("TSLA");
        tesla.setStockName("Tesla Inc.");
        tesla.setCountry(Stock.Country.US);
        tesla.setEnabled(true);
        stockRepository.save(tesla);

        Stock google = new Stock();
        google.setStockCode("GOOGL");
        google.setStockName("Alphabet Inc.");
        google.setCountry(Stock.Country.US);
        google.setEnabled(true);
        stockRepository.save(google);
    }
}


