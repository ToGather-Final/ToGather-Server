//package com.example.trading_service.controller;
//
//import com.example.trading_service.service.TradingService;
//import com.example.trading_service.dto.*;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.security.test.context.support.WithMockUser;
//
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.List;
//import java.util.UUID;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.when;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(TradingController.class)
//class TradingControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private TradingService tradingService;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Test
//    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
//    void 투자계좌_생성_성공() throws Exception {
//        // Given
//        UUID accountId = UUID.randomUUID();
//        when(tradingService.createInvestmentAccount(any(UUID.class))).thenReturn(accountId);
//
//        // When & Then
//        mockMvc.perform(post("/trading/account/invest")
//                .with(csrf()))
//                .andExpect(status().isCreated())
//                .andExpect(content().string("\"" + accountId.toString() + "\""));
//    }
//
//    @Test
//    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
//    void 주식_매수_성공() throws Exception {
//        // Given
//        UUID stockId = UUID.randomUUID();
//        BuyRequest request = new BuyRequest(stockId, 10, 70000.0f, false);
//        doNothing().when(tradingService).buyStock(any(UUID.class), any(BuyRequest.class));
//
//        // When & Then
//        mockMvc.perform(put("/trading/trade/buy")
//                .with(csrf())
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(content().string("매수 주문이 완료되었습니다."));
//    }
//
//    @Test
//    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
//    void 주식_매도_성공() throws Exception {
//        // Given
//        UUID stockId = UUID.randomUUID();
//        SellRequest request = new SellRequest(stockId, 5, 75000.0f, false);
//        doNothing().when(tradingService).sellStock(any(UUID.class), any(SellRequest.class));
//
//        // When & Then
//        mockMvc.perform(put("/trading/trade/sell")
//                .with(csrf())
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(content().string("매도 주문이 완료되었습니다."));
//    }
//
//    @Test
//    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
//    void 예수금_충전_성공() throws Exception {
//        // Given
//        DepositRequest request = new DepositRequest(500000L);
//        doNothing().when(tradingService).depositFunds(any(UUID.class), any(DepositRequest.class));
//
//        // When & Then
//        mockMvc.perform(put("/trading/trade/deposit")
//                .with(csrf())
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(content().string("예수금 충전이 완료되었습니다."));
//    }
//
//    @Test
//    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
//    void 보유종목_조회_성공() throws Exception {
//        // Given
//        UUID stockId = UUID.randomUUID();
//        List<HoldingResponse> holdings = Arrays.asList(
//            new HoldingResponse(
//                UUID.randomUUID(), stockId, "005930", "삼성전자",
//                10, 70000.0f, 75000.0f, 50000.0f, 750000.0f, 7.14f
//            )
//        );
//        when(tradingService.getUserHoldings(any(UUID.class))).thenReturn(holdings);
//
//        // When & Then
//        mockMvc.perform(get("/trading/portfolio/holdings"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].stockCode").value("005930"))
//                .andExpect(jsonPath("$[0].stockName").value("삼성전자"))
//                .andExpect(jsonPath("$[0].quantity").value(10));
//    }
//
//    @Test
//    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
//    void 계좌잔고_조회_성공() throws Exception {
//        // Given
//        UUID accountId = UUID.randomUUID();
//        BalanceResponse balance = new BalanceResponse(
//            UUID.randomUUID(), accountId, 1000000L,
//            700000.0f, 750000.0f, 50000.0f, 7.14f
//        );
//        when(tradingService.getAccountBalance(any(UUID.class))).thenReturn(balance);
//
//        // When & Then
//        mockMvc.perform(get("/trading/account/balance"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.balance").value(1000000))
//                .andExpect(jsonPath("$.totalProfit").value(50000.0))
//                .andExpect(jsonPath("$.totalProfitRate").value(7.14f));
//    }
//
//    @Test
//    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
//    void 거래내역_조회_성공() throws Exception {
//        // Given
//        UUID stockId = UUID.randomUUID();
//        List<TradeHistoryResponse> trades = Arrays.asList(
//            new TradeHistoryResponse(
//                UUID.randomUUID(), stockId, "005930", "삼성전자",
//                "BUY", 10, 70000.0f, LocalDateTime.now(), "FILLED"
//            )
//        );
//        when(tradingService.getTradeHistory(any(UUID.class))).thenReturn(trades);
//
//        // When & Then
//        mockMvc.perform(get("/trading/portfolio/history"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].stockCode").value("005930"))
//                .andExpect(jsonPath("$[0].orderType").value("BUY"))
//                .andExpect(jsonPath("$[0].quantity").value(10));
//    }
//
//    @Test
//    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
//    void 주식목록_조회_성공() throws Exception {
//        // Given
//        UUID stockId = UUID.randomUUID();
//        List<StockResponse> stocks = Arrays.asList(
//            new StockResponse(
//                stockId, "005930", "삼성전자", null, "KR",
//                75000.0f, 2000.0f, 2.74f, true
//            )
//        );
//        when(tradingService.getStocks(anyString())).thenReturn(stocks);
//
//        // When & Then
//        mockMvc.perform(get("/trading/stocks")
//                .param("search", "삼성"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].stockCode").value("005930"))
//                .andExpect(jsonPath("$[0].stockName").value("삼성전자"))
//                .andExpect(jsonPath("$[0].currentPrice").value(75000.0));
//    }
//
//    @Test
//    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
//    void 주식상세_조회_성공() throws Exception {
//        // Given
//        UUID stockId = UUID.randomUUID();
//        StockResponse stock = new StockResponse(
//            stockId, "005930", "삼성전자", null, "KR",
//            75000.0f, 2000.0f, 2.74f, true
//        );
//        when(tradingService.getStockDetail(stockId)).thenReturn(stock);
//
//        // When & Then
//        mockMvc.perform(get("/trading/stocks/{stockId}", stockId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.stockCode").value("005930"))
//                .andExpect(jsonPath("$.stockName").value("삼성전자"))
//                .andExpect(jsonPath("$.currentPrice").value(75000.0));
//    }
//
//    @Test
//    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
//    void 대기주문_조회_성공() throws Exception {
//        // Given
//        UUID stockId = UUID.randomUUID();
//        List<OrderResponse> orders = Arrays.asList(
//            new OrderResponse(
//                UUID.randomUUID(), stockId, "005930", "삼성전자",
//                "BUY", 10, 70000.0f, "PENDING",
//                LocalDateTime.now(), LocalDateTime.now()
//            )
//        );
//        when(tradingService.getPendingOrders(any(UUID.class))).thenReturn(orders);
//
//        // When & Then
//        mockMvc.perform(get("/trading/orders/pending"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].orderType").value("BUY"))
//                .andExpect(jsonPath("$[0].status").value("PENDING"))
//                .andExpect(jsonPath("$[0].quantity").value(10));
//    }
//
//    @Test
//    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
//    void 주문_취소_성공() throws Exception {
//        // Given
//        UUID orderId = UUID.randomUUID();
//        doNothing().when(tradingService).cancelOrder(any(UUID.class), eq(orderId));
//
//        // When & Then
//        mockMvc.perform(delete("/trading/orders/{orderId}", orderId)
//                .with(csrf()))
//                .andExpect(status().isOk())
//                .andExpect(content().string("주문이 취소되었습니다."));
//    }
//
//    @Test
//    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
//    void 계좌정보_조회_성공() throws Exception {
//        // Given
//        UUID accountId = UUID.randomUUID();
//        AccountInfoResponse accountInfo = new AccountInfoResponse(
//            accountId, "INV123456789", "550e8400-e29b-41d4-a716-446655440000",
//            LocalDateTime.now(), true
//        );
//        when(tradingService.getAccountInfo(any(UUID.class))).thenReturn(accountInfo);
//
//        // When & Then
//        mockMvc.perform(get("/trading/account/info"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accountNo").value("INV123456789"))
//                .andExpect(jsonPath("$.hasAccount").value(true));
//    }
//
//    @Test
//    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
//    void 포트폴리오_요약_조회_성공() throws Exception {
//        // Given
//        PortfolioSummaryResponse summary = new PortfolioSummaryResponse(
//            1000000.0f, 1100000.0f, 100000.0f, 10.0f,
//            3, Arrays.asList()
//        );
//        when(tradingService.getPortfolioSummary(any(UUID.class))).thenReturn(summary);
//
//        // When & Then
//        mockMvc.perform(get("/trading/portfolio/summary"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.totalInvested").value(1000000.0))
//                .andExpect(jsonPath("$.totalValue").value(1100000.0))
//                .andExpect(jsonPath("$.totalProfitRate").value(10.0));
//    }
//}
