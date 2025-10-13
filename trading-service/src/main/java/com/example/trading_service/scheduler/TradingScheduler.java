package com.example.trading_service.scheduler;

import com.example.trading_service.domain.Order;
import com.example.trading_service.repository.OrderRepository;
import com.example.trading_service.service.StockPriceService;
import com.example.trading_service.service.TradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TradingScheduler {

    private final StockPriceService stockPriceService;
    private final OrderRepository orderRepository;
    private final TradingService tradingService;

    /**
     * 3초마다 미체결 주문을 조회하고, 지정가 조건을 만족하면 자동 체결
     */
    @Scheduled(fixedRate = 3000)
    public void executePendingOrders() {
        List<Order> pendingOrders = orderRepository.findByStatus(Order.Status.PENDING);
        
        // 디버깅: PENDING 주문 개수 출력
        System.out.println("🔍 [스케줄러] PENDING 주문 개수: " + pendingOrders.size());

        for (Order order : pendingOrders) {
            try {
                System.out.printf("📋 [주문체크] %s %s - 지정가: %.0f원\n", 
                    order.getOrderType(), order.getStockCode(), order.getPrice());
                
                Map<String, Object> result = stockPriceService.getCurrentPrice(order.getStockCode());
                Map<String, Object> output = (Map<String, Object>) result.get("output");
                if (output == null) {
                    System.out.println("⚠️ [주문체크] 현재가 조회 실패 - output이 null");
                    continue;
                }

                long currentPrice = Long.parseLong((String) output.get("stck_prpr"));
                long targetPrice = (long) order.getPrice(); // 주문 지정가
                
                System.out.printf("💰 [주문체크] 현재가: %d원 / 지정가: %d원\n", currentPrice, targetPrice);

                boolean shouldExecute = false;
                if (order.isBuy() && currentPrice >= targetPrice) shouldExecute = true;  // 매수: 현재가 >= 지정가
                if (order.isSell() && currentPrice <= targetPrice) shouldExecute = true; // 매도: 현재가 <= 지정가
                
                System.out.printf("🎯 [주문체크] 체결조건: %s\n", shouldExecute ? "만족" : "불만족");

                if (shouldExecute) {
                    System.out.printf("💰 [체결] %s (%s) 현재가: %d / 지정가: %d\n",
                            order.getStockName(), order.getStockCode(), currentPrice, targetPrice);

                    tradingService.executeOrder(order, currentPrice); // 체결 실행
                    order.setStatus(Order.Status.COMPLETED);
                    orderRepository.save(order);
                }

            } catch (Exception e) {
                System.out.println("⚠️ 주문 처리 중 오류: " + e.getMessage());
                // 예외 발생 시 주문 상태를 CANCELLED로 변경
                order.setStatus(Order.Status.CANCELLED);
                orderRepository.save(order);
            }
        }
    }

    // 예시: 삼성전자(005930) 3초마다 시세 조회 (디버깅용)
    @Scheduled(fixedRate = 3000)
    public void fetchStockPrice() {
        try {
            Map<String, Object> result = stockPriceService.getCurrentPrice("005930");
            Map<String, Object> output = (Map<String, Object>) result.get("output");

            if (output != null) {
                String price = (String) output.get("stck_prpr");
                String diff = (String) output.get("prdy_vrss");
                String rate = (String) output.get("prdy_ctrt");
                System.out.printf("📈 [삼성전자] 현재가: %s원 (%s / %s%%)\n", price, diff, rate);
            } else {
                System.out.println("⚠️ 응답에 output 필드 없음: " + result);
            }

        } catch (Exception e) {
            System.out.println("❌ 시세 조회 실패: " + e.getMessage());
        }
    }
}
