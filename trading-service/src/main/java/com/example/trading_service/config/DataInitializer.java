package com.example.trading_service.config;

import com.example.trading_service.domain.BalanceCache;
import com.example.trading_service.domain.Order;
import com.example.trading_service.domain.Stock;
import com.example.trading_service.repository.BalanceCacheRepository;
import com.example.trading_service.repository.OrderRepository;
import com.example.trading_service.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;
    private final BalanceCacheRepository balanceCacheRepository;

    @Override
    public void run(String... args) throws Exception {
        try {
            // 샘플 주식 데이터가 없으면 생성
            if (stockRepository.count() == 0) {
                createSampleStocks();
                log.info("샘플 주식 데이터가 생성되었습니다.");
            }
            
            // 테스트용 주문 데이터 생성
            createTestOrders();
        } catch (Exception e) {
            log.warn("데이터베이스 초기화 중 오류 발생: {}", e.getMessage());
            log.info("API 서비스는 정상적으로 작동합니다.");
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

    private void createTestOrders() {
        // 테스트용 투자 계좌 ID (실제로는 존재해야 함)
        UUID testAccountId = UUID.randomUUID();
        
        // 실제 삼성전자 Stock ID 조회
        Stock samsungStock = stockRepository.findByStockCode("005930")
                .orElseThrow(() -> new RuntimeException("삼성전자 주식 데이터를 찾을 수 없습니다."));
        UUID testStockId = samsungStock.getId();
        
        // 테스트용 초기 잔고 설정 (100만원)
        BalanceCache testBalance = new BalanceCache();
        testBalance.setInvestmentAccountId(testAccountId);
        testBalance.setBalance(1000000); // 100만원
        balanceCacheRepository.save(testBalance);
        
        // 매수 주문: 현재가(94000원)보다 높은 가격으로 주문 (체결)
        Order buyOrder = new Order();
        buyOrder.setInvestmentAccountId(testAccountId);
        buyOrder.setStockId(testStockId);
        buyOrder.setOrderType(Order.OrderType.BUY);
        buyOrder.setQuantity(10);
        buyOrder.setPrice(95000); // 현재가보다 높음
        buyOrder.setStatus(Order.Status.PENDING);
        orderRepository.save(buyOrder);
        
        // 매수 주문: 현재가(94000원)보다 낮은 가격으로 주문 (체결 X)
        Order buyOrder2 = new Order();
        buyOrder2.setInvestmentAccountId(testAccountId);
        buyOrder2.setStockId(testStockId);
        buyOrder2.setOrderType(Order.OrderType.BUY);
        buyOrder2.setQuantity(5);
        buyOrder2.setPrice(93000); // 현재가보다 낮음
        buyOrder2.setStatus(Order.Status.PENDING);
        orderRepository.save(buyOrder2);
        
        // 매도 주문: 현재가(94000원)보다 높은 가격으로 주문 (체결)
        Order sellOrder = new Order();
        sellOrder.setInvestmentAccountId(testAccountId);
        sellOrder.setStockId(testStockId);
        sellOrder.setOrderType(Order.OrderType.SELL);
        sellOrder.setQuantity(3); // 5주 보유 중 3주만 매도
        sellOrder.setPrice(96000); // 현재가보다 높음
        sellOrder.setStatus(Order.Status.PENDING);
        orderRepository.save(sellOrder);
        
        // 매도 주문: 현재가(94000원)보다 낮은 가격으로 주문 (체결 X)
        Order sellOrder2 = new Order();
        sellOrder2.setInvestmentAccountId(testAccountId);
        sellOrder2.setStockId(testStockId);
        sellOrder2.setOrderType(Order.OrderType.SELL);
        sellOrder2.setQuantity(2); // 5주 보유 중 2주 매도
        sellOrder2.setPrice(92000); // 현재가보다 낮음
        sellOrder2.setStatus(Order.Status.PENDING);
        orderRepository.save(sellOrder2);
        
        log.info("테스트 주문 데이터가 생성되었습니다.");
    }
}


