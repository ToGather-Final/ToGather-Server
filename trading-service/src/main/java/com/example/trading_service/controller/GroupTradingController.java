package com.example.trading_service.controller;

import com.example.trading_service.dto.ApiResponse;
import com.example.trading_service.dto.GroupBuyRequest;
import com.example.trading_service.dto.GroupSellRequest;
import com.example.trading_service.service.GroupTradingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/group-trading")
@RequiredArgsConstructor
@Slf4j
public class GroupTradingController {

    private final GroupTradingService groupTradingService;

    /**
     * 그룹 매수 주문
     */
    @PostMapping("/buy")
    public ResponseEntity<ApiResponse<Integer>> groupBuyOrder(
            @Valid @RequestBody GroupBuyRequest request,
            Authentication authentication) {
        
        log.info("그룹 매수 주문 요청 - 그룹ID: {}, 주식ID: {}, 수량: {}, 가격: {}", 
                request.getGroupId(), request.getStockId(), request.getQuantity(), request.getPrice());

        try {
            int processedCount = groupTradingService.processGroupBuyOrder(
                    request.getGroupId(),
                    request.getStockId(),
                    request.getQuantity(),
                    request.getPrice()
            );

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("그룹 매수 주문이 완료되었습니다. 처리된 주문 수: %d", processedCount),
                    processedCount
            ));

        } catch (Exception e) {
            log.error("그룹 매수 주문 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage(), "GROUP_BUY_ORDER_FAILED")
            );
        }
    }

    /**
     * 그룹 매도 주문
     */
    @PostMapping("/sell")
    public ResponseEntity<ApiResponse<Integer>> groupSellOrder(
            @Valid @RequestBody GroupSellRequest request,
            Authentication authentication) {
        
        log.info("그룹 매도 주문 요청 - 그룹ID: {}, 주식ID: {}, 수량: {}, 가격: {}", 
                request.getGroupId(), request.getStockId(), request.getQuantity(), request.getPrice());

        try {
            int processedCount = groupTradingService.processGroupSellOrder(
                    request.getGroupId(),
                    request.getStockId(),
                    request.getQuantity(),
                    request.getPrice()
            );

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("그룹 매도 주문이 완료되었습니다. 처리된 주문 수: %d", processedCount),
                    processedCount
            ));

        } catch (Exception e) {
            log.error("그룹 매도 주문 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage(), "GROUP_SELL_ORDER_FAILED")
            );
        }
    }

    /**
     * 그룹 보유 현황 조회
     */
    @GetMapping("/holdings/{groupId}")
    public ResponseEntity<ApiResponse<Object>> getGroupHoldings(@PathVariable UUID groupId) {
        // TODO: 그룹 보유 현황 조회 로직 구현
        return ResponseEntity.ok(ApiResponse.success("그룹 보유 현황 조회 기능은 추후 구현 예정입니다."));
    }
}


