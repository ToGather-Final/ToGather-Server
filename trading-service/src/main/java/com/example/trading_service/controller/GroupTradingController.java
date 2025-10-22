package com.example.trading_service.controller;

import com.example.trading_service.dto.ApiResponse;
import com.example.trading_service.dto.GroupBuyRequest;
import com.example.trading_service.dto.GroupHoldingResponse;
import com.example.trading_service.dto.GroupSellRequest;
import com.example.trading_service.service.GroupTradingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/group-trading")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "그룹 거래", description = "그룹 투자 거래 관련 API")
public class GroupTradingController {

    private final GroupTradingService groupTradingService;

    @Operation(summary = "그룹 매수 주문", description = "그룹 멤버들의 투자 계좌에 동일한 매수 주문을 생성합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "그룹 매수 주문 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
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
                    String.format("그룹 매수 주문이 생성되었습니다. (생성된 주문 수: %d, 상태: PENDING, 호가 조건 만족 시 자동 체결)", processedCount),
                    processedCount
            ));

        } catch (Exception e) {
            log.error("그룹 매수 주문 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage(), "GROUP_BUY_ORDER_FAILED")
            );
        }
    }

    @Operation(summary = "그룹 매도 주문", description = "그룹 멤버들의 투자 계좌에 동일한 매도 주문을 생성합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "그룹 매도 주문 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
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
                    String.format("그룹 매도 주문이 생성되었습니다. (생성된 주문 수: %d, 상태: PENDING, 호가 조건 만족 시 자동 체결)", processedCount),
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
    @Operation(summary = "그룹 보유 현황 조회", description = "특정 그룹의 보유 종목 현황을 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "그룹 보유 현황 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "그룹 보유 현황 조회 실패")
    })
    @GetMapping("/holdings/{groupId}")
    public ResponseEntity<ApiResponse<List<GroupHoldingResponse>>> getGroupHoldings(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId) {
        log.info("그룹 보유 현황 조회 요청 - 그룹ID: {}", groupId);
        
        try {
            List<GroupHoldingResponse> holdings = groupTradingService.getGroupHoldings(groupId);
            return ResponseEntity.ok(ApiResponse.success(holdings));
        } catch (Exception e) {
            log.error("그룹 보유 현황 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("그룹 보유 현황 조회 실패: " + e.getMessage(), "GROUP_HOLDINGS_ERROR")
            );
        }
    }
}


