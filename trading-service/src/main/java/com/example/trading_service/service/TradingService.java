package com.example.trading_service.service;

import com.example.module_common.dto.InvestmentAccountDto;
import com.example.module_common.dto.pay.PayRechargeRequest;
import com.example.module_common.dto.pay.PayRechargeResponse;
import com.example.module_common.dto.vote.VoteTradingRequest;
import com.example.module_common.dto.vote.VoteTradingResponse;
import com.example.trading_service.client.PayServiceClient;
import com.example.trading_service.domain.*;
import com.example.trading_service.dto.*;
import com.example.trading_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trading_service.util.AccountNumberGenerator;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TradingService {

    private final InvestmentAccountRepository investmentAccountRepository;
    private final BalanceCacheRepository balanceCacheRepository;
    private final HoldingCacheRepository holdingCacheRepository;
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final StockRepository stockRepository;
    private final StockPriceService stockPriceService;
    private final ChartService chartService;
    private final OrderService orderService;
    private final PortfolioCalculationService portfolioCalculationService;
    private final PayServiceClient payServiceClient;
    private final HistoryRepository historyRepository;
    private final VoteTradingService voteTradingService;

    // 투자 계좌 개설
    public UUID createInvestmentAccount(UUID userId) {
        // 이미 계좌가 있는지 확인
        Optional<InvestmentAccount> existingAccount = investmentAccountRepository.findByUserId(userId);
        
        if (existingAccount.isPresent()) {
            // 계좌는 있지만 BalanceCache가 없는 경우 생성
            InvestmentAccount account = existingAccount.get();
            Optional<BalanceCache> existingBalance = balanceCacheRepository.findByAccountId(account.getInvestmentAccountId());
            
            if (existingBalance.isEmpty()) {
                log.warn("⚠️ 투자 계좌는 있지만 BalanceCache가 없음 - userId: {}, accountId: {}. BalanceCache 생성 중...", 
                        userId, account.getInvestmentAccountId());
                
                BalanceCache balance = new BalanceCache();
                balance.setInvestmentAccount(account);
                balance.setBalance(0);
                balanceCacheRepository.save(balance);
                
                log.info("✅ BalanceCache 생성 완료 - userId: {}, accountId: {}", userId, account.getInvestmentAccountId());
            } else {
                log.info("✅ 기존 투자 계좌 확인 - userId: {}, 계좌번호: {}", userId, account.getAccountNo());
            }
            
            return account.getInvestmentAccountId();
        }

        // 계좌 생성
        InvestmentAccount account = new InvestmentAccount();
        log.info("🔍 투자 계좌 생성 - userId: {}, 타입: {}", userId, userId.getClass().getName());
        account.setUserId(userId);
        account.setAccountNo(generateAccountNumber());
        
        InvestmentAccount savedAccount = investmentAccountRepository.save(account);
        
        // 초기 잔고 생성
        BalanceCache balance = new BalanceCache();
        balance.setInvestmentAccount(savedAccount);
        balance.setBalance(0);
        balanceCacheRepository.save(balance);
        
        log.info("✅ 투자 계좌가 생성되었습니다. 사용자: {}, 계좌번호: {}", userId, savedAccount.getAccountNo());
        return savedAccount.getInvestmentAccountId();
    }

    // 주식 매수 (OrderService로 위임)
    public void buyStock(UUID userId, BuyRequest request) {
        orderService.buyStock(userId, request);
    }

    // 주식 매도 (OrderService로 위임)
    public void sellStock(UUID userId, SellRequest request) {
        orderService.sellStock(userId, request);
    }

    // 예수금 충전
    public void depositFunds(UUID userId, DepositRequest request) {
        // 투자 계좌 조회
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        // 잔고 업데이트
        BalanceCache balance = balanceCacheRepository.findByAccountId(account.getInvestmentAccountId())
                .orElseThrow(() -> new IllegalArgumentException("잔고 정보를 찾을 수 없습니다."));
        
        balance.setBalance(balance.getBalance() + request.getAmount().intValue());
        balanceCacheRepository.save(balance);
        
        
        // History 테이블에 현금 입금 완료 히스토리 저장 (일단 주석 처리)
        /*
        try {
            // 개인 거래의 경우 사용자 ID를 기반으로 임시 그룹 ID 생성
            UUID tempGroupId = UUID.nameUUIDFromBytes(("personal_" + userId.toString()).getBytes());
            
            if (tempGroupId != null) {
                String payload = String.format(
                    "{\"amount\":%d,\"accountBalance\":%d}",
                    request.getAmount().intValue(),
                    balance.getBalance()
                );
                
                String title = String.format("현금 입금 완료 - %d원", request.getAmount().intValue());
                
                History history = History.create(
                    tempGroupId,
                    HistoryCategory.CASH,
                    HistoryType.CASH_DEPOSIT_COMPLETED,
                    title,
                    payload
                );
                
                historyRepository.save(history);
                
                log.info("현금 입금 완료 히스토리 저장 완료 - 임시그룹ID: {}, 금액: {}", 
                        tempGroupId, request.getAmount().intValue());
            }
        } catch (Exception e) {
            log.error("현금 입금 완료 히스토리 저장 실패 - 사용자: {} - {}", userId, e.getMessage());
        }
        */
        
        log.info("예수금이 충전되었습니다. 사용자: {}, 충전 금액: {}", userId, request.getAmount());
    }

    /**
     * Internal 예수금 충전 (서비스 간 통신용)
     * - vote-service에서 PAY 투표 가결 시 자동으로 호출
     * - 인증 없이 직접 userId로 처리
     */
    @Transactional
    public void internalDepositFunds(InternalDepositRequest request) {
        UUID userId = request.getUserId();
        
        // 투자 계좌 조회 (계좌가 없으면 예외 발생)
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        // 잔고 업데이트 (BalanceCache가 없으면 자동 생성)
        BalanceCache balance = balanceCacheRepository.findByAccountId(account.getInvestmentAccountId())
                .orElseGet(() -> {
                    log.warn("⚠️ BalanceCache가 없음 - userId: {}, accountId: {}. 자동 생성 중...", 
                            userId, account.getInvestmentAccountId());
                    
                    BalanceCache newBalance = new BalanceCache();
                    newBalance.setInvestmentAccount(account);
                    newBalance.setBalance(0);
                    BalanceCache saved = balanceCacheRepository.save(newBalance);
                    
                    log.info("✅ BalanceCache 자동 생성 완료 - userId: {}, accountId: {}", 
                            userId, account.getInvestmentAccountId());
                    
                    return saved;
                });
        
        balance.setBalance(balance.getBalance() + request.getAmount().intValue());
        balanceCacheRepository.save(balance);
        
        log.info("✅ Internal 예수금 충전 완료 - 사용자: {}, 그룹: {}, 충전 금액: {}원, 현재 잔고: {}원, 설명: {}", 
                userId, request.getGroupId(), request.getAmount(), balance.getBalance(), request.getDescription());
    }

    /**
     * 투표 기반 거래 실행
     * - TRADE 투표 가결 시 자동으로 거래 실행
     */
    @Transactional
    public VoteTradingResponse executeVoteBasedTrading(VoteTradingRequest request) {
        try {
            log.info("투표 기반 거래 실행 시작 - proposalId: {}, groupId: {}, stockId: {}, action: {}, quantity: {}, price: {}", 
                    request.proposalId(), request.groupId(), request.stockId(), request.tradingAction(), 
                    request.quantity(), request.price());

            int processedCount = voteTradingService.executeVoteBasedTrading(request);

            log.info("투표 기반 거래 실행 완료 - proposalId: {}, 처리된 거래 수: {}", request.proposalId(), processedCount);
            
            return new VoteTradingResponse(true, "투표 기반 거래가 성공적으로 실행되었습니다", processedCount);
            
        } catch (Exception e) {
            log.error("투표 기반 거래 실행 실패 - proposalId: {}, 오류: {}", request.proposalId(), e.getMessage(), e);
            return new VoteTradingResponse(false, "투표 기반 거래 실행 실패: " + e.getMessage(), 0);
        }
    }

    /**
     * 그룹 예수금 총합 조회
     * - 그룹 멤버들의 예수금 잔액 합계
     */
    @Transactional(readOnly = true)
    public Integer getGroupTotalBalance(List<UUID> memberIds) {
        try {
            log.info("그룹 예수금 총합 조회 시작 - memberCount: {}", memberIds.size());
            
            Integer totalBalance = 0;
            
            // 각 멤버의 예수금 잔액 조회 및 합산
            for (UUID memberId : memberIds) {
                try {
                    // 투자 계좌 조회
                    Optional<InvestmentAccount> accountOpt = investmentAccountRepository.findByUserId(memberId);
                    if (accountOpt.isPresent()) {
                        InvestmentAccount account = accountOpt.get();
                        
                        // 잔고 캐시 조회
                        Optional<BalanceCache> balanceOpt = balanceCacheRepository.findByAccountId(account.getInvestmentAccountId());
                        if (balanceOpt.isPresent()) {
                            BalanceCache balance = balanceOpt.get();
                            totalBalance += balance.getBalance();
                            log.debug("멤버 예수금 잔액 - memberId: {}, balance: {}", memberId, balance.getBalance());
                        }
                    }
                } catch (Exception e) {
                    log.warn("멤버 예수금 조회 실패 - memberId: {}, 오류: {}", memberId, e.getMessage());
                    // 개별 멤버 조회 실패는 전체 프로세스를 중단하지 않음
                }
            }
            
            log.info("그룹 예수금 총합 조회 완료 - memberCount: {}, totalBalance: {}", 
                    memberIds.size(), totalBalance);
            return totalBalance;
            
        } catch (Exception e) {
            log.error("그룹 예수금 총합 조회 실패 - memberCount: {}, 오류: {}", memberIds.size(), e.getMessage(), e);
            return 0; // 실패 시 기본값 반환
        }
    }

    // 계좌 잔고 조회 (PortfolioCalculationService로 위임)
    @Transactional(readOnly = true)
    public BalanceResponse getAccountBalance(UUID userId) {
        return portfolioCalculationService.calculateAccountBalance(userId);
    }

    // 거래 내역 조회
    @Transactional(readOnly = true)
    public List<TradeHistoryResponse> getTradeHistory(UUID userId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        List<Trade> trades = tradeRepository.findByInvestmentAccountId(account.getInvestmentAccountId());
        
        return trades.stream()
                .map(this::convertToTradeHistoryResponse)
                .collect(Collectors.toList());
    }

    // 주식 조회
    @Transactional(readOnly = true)
    public List<StockResponse> getStocks(String search) {
        List<Stock> stocks;
        
        if (search != null && !search.trim().isEmpty()) {
            stocks = stockRepository.searchStocks(search.trim());
        } else {
            stocks = stockRepository.findByEnabledTrue();
        }
        
        return stocks.stream()
                .map(this::convertToStockResponse)
                .collect(Collectors.toList());
    }

    // 주식 기본 정보 조회 (현재가, 변동률, 거래량 등)
    @Transactional(readOnly = true)
    public StockInfoResponse getStockInfoByCode(String stockCode) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("주식을 찾을 수 없습니다: " + stockCode));

        // 실시간 가격 정보 조회
        StockPriceResponse priceInfo = stockPriceService.getCachedStockPrice(stock.getId(), stockCode, stock.getPrdtTypeCd());
        
        // 간단한 차트 데이터 조회 (80일)
        List<ChartData> chartData = chartService.getStockChart(stockCode, 80);
        
        // 기존 형식으로 변환 (date, volume 필드명)
        List<SimpleChartData> convertedChartData = chartData.stream()
                .map(data -> new SimpleChartData(
                    data.getTime(),           // time → date
                    data.getOpen(),
                    data.getHigh(),
                    data.getLow(),
                    data.getClose(),
                    data.getTrading_volume()  // trading_volume → volume
                ))
                .collect(Collectors.toList());

        return StockInfoResponse.builder()
                .stockId(stock.getId().toString())
                .stockCode(stock.getStockCode())
                .stockName(stock.getStockName())
                .market(stock.getMarket().name())
                .currentPrice(priceInfo.getCurrentPrice())
                .changeAmount(priceInfo.getChangePrice())
                .changeRate(priceInfo.getChangeRate())
                .changeDirection(priceInfo.getChangePrice().compareTo(BigDecimal.ZERO) > 0 ? "up" : 
                               priceInfo.getChangePrice().compareTo(BigDecimal.ZERO) < 0 ? "down" : "unchanged")
                .volume(priceInfo.getVolume())
                .highPrice(priceInfo.getHighPrice())
                .lowPrice(priceInfo.getLowPrice())
                .openPrice(priceInfo.getOpenPrice())
                .prevClosePrice(priceInfo.getPrevClosePrice())
                .marketCap(null) // TODO: 시가총액 계산 로직 추가
                .chartData(convertedChartData)
                .resistanceLine(calculateResistanceLine(chartData))
                .supportLine(calculateSupportLine(chartData))
                .build();
    }

    // 주식 차트 정보 조회 (기본 정보 + 차트 데이터)
    @Transactional(readOnly = true)
    public StockInfoResponse getStockChartWithInfo(String stockCode, String periodDiv) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("주식을 찾을 수 없습니다: " + stockCode));

        // 실시간 가격 정보 조회
        StockPriceResponse priceInfo = stockPriceService.getCachedStockPrice(stock.getId(), stockCode, stock.getPrdtTypeCd());
        
        // 차트 데이터 조회 (기간분류코드 사용)
        List<ChartData> chartData = chartService.getStockChartByPeriod(stockCode, periodDiv);

        return StockInfoResponse.builder()
                .stockId(stock.getId().toString())
                .stockCode(stock.getStockCode())
                .stockName(stock.getStockName())
                .market(stock.getMarket().name())
                .currentPrice(priceInfo.getCurrentPrice())
                .changeAmount(priceInfo.getChangePrice())
                .changeRate(priceInfo.getChangeRate())
                .changeDirection(priceInfo.getChangePrice().compareTo(BigDecimal.ZERO) > 0 ? "up" : 
                               priceInfo.getChangePrice().compareTo(BigDecimal.ZERO) < 0 ? "down" : "unchanged")
                .volume(priceInfo.getVolume())
                .highPrice(priceInfo.getHighPrice())
                .lowPrice(priceInfo.getLowPrice())
                .openPrice(priceInfo.getOpenPrice())
                .prevClosePrice(priceInfo.getPrevClosePrice())
                .marketCap(null)
                .chartData(chartData)
                .resistanceLine(calculateResistanceLine(chartData))
                .supportLine(calculateSupportLine(chartData))
                .build();
    }

    // 주식 코드로 상세 정보 조회 (차트 데이터 포함)
    @Transactional(readOnly = true)
    public StockDetailResponse getStockDetailByCode(String stockCode) {
        return getStockDetailByCode(stockCode, 30);
    }

    // 주식 코드로 상세 정보 조회 (차트 데이터 포함, 기간 지정)
    @Transactional(readOnly = true)
    public StockDetailResponse getStockDetailByCode(String stockCode, int days) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주식입니다: " + stockCode));
        
        return convertToStockDetailResponse(stock, days);
    }

    public void rechargeGroupPayAccount(UUID groupId, Long amount) {
        PayRechargeRequest request = new PayRechargeRequest(amount, UUID.randomUUID().toString());
        PayRechargeResponse response = payServiceClient.rechargePayMoney(
                request,
                getCurrentUserId(),
                groupId
        );
        log.info("그룹 페이 계좌 충전 완료: {}", response);
    }

    @Transactional(readOnly = true)
    public InvestmentAccountDto getAccountByUserIdInternal(UUID userId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);

        return InvestmentAccountDto.builder()
                .investmentAccountId(account.getInvestmentAccountId())
                .userId(account.getUserId())
                .accountNo(account.getAccountNo())
                .createdAt(account.getCreatedAt())
                .build();
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UUID) {
            return (UUID) authentication.getPrincipal();
        }
        return null;
    }

    // 시장가 주문 처리
    private void processMarketOrder(Order order) {
        // 실제 거래소에서는 실시간 가격을 가져와야 하지만, 
        // 여기서는 주문 가격으로 즉시 체결 처리
        executeTrade(order, order.getPrice());
    }

    // 체결 처리
    private void executeTrade(Order order, float executionPrice) {
        // 체결 기록 생성
        Trade trade = new Trade();
        trade.setOrder(order);
        trade.setQuantity(order.getQuantity());
        trade.setPrice(executionPrice);
        tradeRepository.save(trade);

        // 주문 상태 업데이트
        order.setStatus(Order.Status.FILLED);
        orderRepository.save(order);

        // 잔고 및 보유 종목 업데이트
        updateAccountAfterTrade(order, executionPrice);
    }

    // 거래 후 계좌 업데이트
    private void updateAccountAfterTrade(Order order, float executionPrice) {
        float totalAmount = executionPrice * order.getQuantity();
        
        if (order.getOrderType() == Order.OrderType.BUY) {
            // 매수: 잔고 차감, 보유 종목 추가/업데이트
            updateBalance(order.getInvestmentAccount().getInvestmentAccountId(), -totalAmount);
            updateHolding(order.getInvestmentAccount().getInvestmentAccountId(), order.getStock().getId(), order.getQuantity(), executionPrice, true);
        } else {
            // 매도: 잔고 증가, 보유 종목 차감
            updateBalance(order.getInvestmentAccount().getInvestmentAccountId(), totalAmount);
            updateHolding(order.getInvestmentAccount().getInvestmentAccountId(), order.getStock().getId(), order.getQuantity(), executionPrice, false);
        }
    }

    // 잔고 업데이트
    private void updateBalance(UUID accountId, float amount) {
        BalanceCache balance = balanceCacheRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("잔고 정보를 찾을 수 없습니다."));
        
        balance.setBalance(balance.getBalance() + (int) amount);
        balanceCacheRepository.save(balance);
    }

    // 보유 종목 업데이트
    private void updateHolding(UUID accountId, UUID stockId, float quantity, float price, boolean isBuy) {
        Optional<HoldingCache> existingHolding = holdingCacheRepository
                .findByAccountIdAndStockId(accountId, stockId);
        
        if (isBuy) {
            // 매수
            if (existingHolding.isPresent()) {
                HoldingCache holding = existingHolding.get();
                float newAvgCost = ((holding.getAvgCost() * holding.getQuantity()) + (price * quantity)) 
                        / (holding.getQuantity() + quantity);
                holding.setQuantity(holding.getQuantity() + quantity);
                holding.setAvgCost(newAvgCost);
                holdingCacheRepository.save(holding);
            } else {
                HoldingCache newHolding = new HoldingCache();
                InvestmentAccount account = investmentAccountRepository.findById(accountId)
                        .orElseThrow(() -> new RuntimeException("투자 계좌를 찾을 수 없습니다"));
                Stock stock = stockRepository.findById(stockId)
                        .orElseThrow(() -> new RuntimeException("주식을 찾을 수 없습니다"));
                
                newHolding.setInvestmentAccount(account);
                newHolding.setStock(stock);
                newHolding.setQuantity(quantity);
                newHolding.setAvgCost(price);
                holdingCacheRepository.save(newHolding);
            }
        } else {
            // 매도
            if (existingHolding.isPresent()) {
                HoldingCache holding = existingHolding.get();
                holding.setQuantity(holding.getQuantity() - quantity);
                if (holding.getQuantity() <= 0) {
                    holdingCacheRepository.delete(holding);
                } else {
                    holdingCacheRepository.save(holding);
                }
            }
        }
    }

    // 헬퍼 메서드들
    private InvestmentAccount getInvestmentAccountByUserId(UUID userId) {
        return investmentAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("투자 계좌를 찾을 수 없습니다."));
    }

    private String generateAccountNumber() {
        return AccountNumberGenerator.generateAccountNumber();
    }

    // 실시간 주식 가격 조회
    private float getCurrentStockPrice(String stockCode) {
        try {
            Map<String, Object> priceData = stockPriceService.getCurrentPrice(stockCode);
            
            // 한투 API 응답에서 현재가 추출
            if (priceData != null && priceData.containsKey("output")) {
                Map<String, Object> output = (Map<String, Object>) priceData.get("output");
                if (output != null && output.containsKey("stck_prpr")) {
                    String priceStr = (String) output.get("stck_prpr");
                    return Float.parseFloat(priceStr.replace(",", ""));
                }
            }
            
            log.warn("주식 가격 정보를 가져올 수 없습니다. 종목코드: {}", stockCode);
            return 0.0f;
        } catch (Exception e) {
            log.error("주식 가격 조회 중 오류 발생. 종목코드: {}, 오류: {}", stockCode, e.getMessage());
            return 0.0f;
        }
    }

    // 주식 변동률 정보 조회
    private Map<String, Float> getStockChangeInfo(String stockCode) {
        try {
            Map<String, Object> priceData = stockPriceService.getCurrentPrice(stockCode);
            
            if (priceData != null && priceData.containsKey("output")) {
                Map<String, Object> output = (Map<String, Object>) priceData.get("output");
                if (output != null) {
                    float changeAmount = 0.0f;
                    float changeRate = 0.0f;
                    
                    if (output.containsKey("prdy_vrss")) {
                        String changeAmountStr = (String) output.get("prdy_vrss");
                        changeAmount = Float.parseFloat(changeAmountStr.replace(",", ""));
                    }
                    
                    if (output.containsKey("prdy_vrss_sign")) {
                        String sign = (String) output.get("prdy_vrss_sign");
                        if ("2".equals(sign)) { // 하락
                            changeAmount = -Math.abs(changeAmount);
                        }
                    }
                    
                    if (output.containsKey("prdy_ctrt")) {
                        String changeRateStr = (String) output.get("prdy_ctrt");
                        changeRate = Float.parseFloat(changeRateStr.replace(",", ""));
                        
                        if (changeAmount < 0) {
                            changeRate = -Math.abs(changeRate);
                        }
                        
                        // 소수점 두 자리로 반올림
                        changeRate = Math.round(changeRate * 100.0f) / 100.0f;
                    }
                    
                    return Map.of("changeAmount", changeAmount, "changeRate", changeRate);
                }
            }
            
            log.warn("주식 변동률 정보를 가져올 수 없습니다. 종목코드: {}", stockCode);
            return Map.of("changeAmount", 0.0f, "changeRate", 0.0f);
        } catch (Exception e) {
            log.error("주식 변동률 조회 중 오류 발생. 종목코드: {}, 오류: {}", stockCode, e.getMessage());
            return Map.of("changeAmount", 0.0f, "changeRate", 0.0f);
        }
    }


    private TradeHistoryResponse convertToTradeHistoryResponse(Trade trade) {
        // Order와 Stock 정보 조회
        Order order = trade.getOrder();
        Stock stock = order.getStock();
        
        return new TradeHistoryResponse(
                trade.getTradeId(),
                stock.getId(),
                stock.getStockCode() != null ? stock.getStockCode() : "",
                stock.getStockName() != null ? stock.getStockName() : "",
                order.getOrderType().toString(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getCreatedAt(),
                "FILLED"
        );
    }

    private StockResponse convertToStockResponse(Stock stock) {
        // 캐시된 주식 가격 정보 조회 (일관성 보장)
        StockPriceResponse priceInfo = stockPriceService.getCachedStockPrice(stock.getId(), stock.getStockCode(), stock.getPrdtTypeCd());
        
        return new StockResponse(
                stock.getId(),
                stock.getStockCode(),
                stock.getStockName(),
                stock.getStockImage(),
                stock.getCountry().toString(),
                stock.getPrdtTypeCd(), // 주식 (300), ETF (500)
                priceInfo.getCurrentPrice().floatValue(),
                priceInfo.getChangePrice().floatValue(),
                priceInfo.getChangeRate(),
                stock.isEnabled()
        );
    }

    // StockDetailResponse 변환 (차트 데이터 포함)
    private StockDetailResponse convertToStockDetailResponse(Stock stock) {
        return convertToStockDetailResponse(stock, 30);
    }

    // StockDetailResponse 변환 (차트 데이터 포함, 기간 지정)
    private StockDetailResponse convertToStockDetailResponse(Stock stock, int days) {
        try {
            // 주식 상세 정보 조회
            Map<String, Object> detailData = stockPriceService.getStockDetail(stock.getStockCode(), stock.getPrdtTypeCd());
            Map<String, Object> output = (Map<String, Object>) detailData.get("output");
            
            if (output == null) {
                log.warn("주식 상세 정보를 가져올 수 없습니다. 종목코드: {}", stock.getStockCode());
                return createEmptyStockDetailResponse(stock);
            }
            
            // 기본 정보 추출
            float currentPrice = parseFloat(output.get("stck_prpr"));
            float changeAmount = parseFloat(output.get("prdy_vrss"));
            float changeRate = parseFloat(output.get("prdy_ctrt"));
            
            // 소수점 두 자리로 반올림
            changeRate = Math.round(changeRate * 100.0f) / 100.0f;
            long volume = parseLong(output.get("acml_vol"));
            float highPrice = parseFloat(output.get("stck_hgpr"));
            float lowPrice = parseFloat(output.get("stck_lwpr"));
            float openPrice = parseFloat(output.get("stck_oprc"));
            float prevClosePrice = parseFloat(output.get("stck_sdpr"));
            
            // 변동 방향 결정
            String changeDirection = "unchanged";
            if (changeAmount > 0) {
                changeDirection = "up";
            } else if (changeAmount < 0) {
                changeDirection = "down";
            }
            
            // 차트 데이터 조회
            List<ChartData> chartData = chartService.getStockChart(stock.getStockCode(), days);
            
            // 저항선/지지선 계산 (임시로 고가/저가 사용)
            float resistanceLine = highPrice * 1.1f; // 고가의 110%
            float supportLine = lowPrice * 0.9f; // 저가의 90%
            
            return new StockDetailResponse(
                    stock.getId(),
                    stock.getStockCode(),
                    stock.getStockName(),
                    stock.getMarket().name(),
                    currentPrice,
                    changeAmount,
                    changeRate,
                    changeDirection,
                    volume,
                    highPrice,
                    lowPrice,
                    openPrice,
                    prevClosePrice,
                    null, // 시가총액 (추후 계산)
                    chartData,
                    resistanceLine,
                    supportLine
            );
            
        } catch (Exception e) {
            log.error("주식 상세 정보 조회 중 오류 발생. 종목코드: {}, 오류: {}", stock.getStockCode(), e.getMessage());
            return createEmptyStockDetailResponse(stock);
        }
    }


    // 빈 StockDetailResponse 생성
    private StockDetailResponse createEmptyStockDetailResponse(Stock stock) {
        return new StockDetailResponse(
                stock.getId(),
                stock.getStockCode(),
                stock.getStockName(),
                stock.getMarket().name(),
                0.0f, 0.0f, 0.0f, "unchanged",
                0L, 0.0f, 0.0f, 0.0f, 0.0f, null,
                List.of(), 0.0f, 0.0f
        );
    }

    // 유틸리티 메서드들
    private float parseFloat(Object value) {
        if (value == null) return 0.0f;
        try {
            String str = value.toString().replace(",", "");
            return Float.parseFloat(str);
        } catch (Exception e) {
            return 0.0f;
        }
    }

    private long parseLong(Object value) {
        if (value == null) return 0L;
        try {
            String str = value.toString().replace(",", "");
            return Long.parseLong(str);
        } catch (Exception e) {
            return 0L;
        }
    }

    // 대기 중인 주문 조회 (OrderService로 위임)
    @Transactional(readOnly = true)
    public List<OrderResponse> getPendingOrders(UUID userId) {
        return orderService.getPendingOrders(userId);
    }

    // 주문 취소 (OrderService로 위임)
    public void cancelOrder(UUID userId, UUID orderId) {
        orderService.cancelOrder(userId, orderId);
    }

    // 계좌 정보 조회
    @Transactional(readOnly = true)
    public AccountInfoResponse getAccountInfo(UUID userId) {
        Optional<InvestmentAccount> accountOpt = investmentAccountRepository.findByUserId(userId);
        
        if (accountOpt.isPresent()) {
            InvestmentAccount account = accountOpt.get();
            return new AccountInfoResponse(
                    account.getInvestmentAccountId(),
                    account.getAccountNo(),
                    account.getUserId(),
                    account.getCreatedAt(),
                    true
            );
        } else {
            return new AccountInfoResponse(null, null, userId, null, false);
        }
    }

    // 특정 종목 거래 내역 조회
    @Transactional(readOnly = true)
    public List<TradeHistoryResponse> getStockTradeHistory(UUID userId, String stockCode) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        // stockCode로 stockId 찾기
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("주식을 찾을 수 없습니다: " + stockCode));
        
        List<Trade> trades = tradeRepository.findByInvestmentAccountIdAndStockId(account.getInvestmentAccountId(), stock.getId());
        
        return trades.stream()
                .map(this::convertToTradeHistoryResponse)
                .collect(Collectors.toList());
    }

    // 저항선 계산
    private BigDecimal calculateResistanceLine(List<ChartData> chartData) {
        if (chartData == null || chartData.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return chartData.stream()
                .map(ChartData::getHigh)
                .max(Float::compareTo)
                .map(BigDecimal::valueOf)
                .orElse(BigDecimal.ZERO);
    }

    // 지지선 계산
    private BigDecimal calculateSupportLine(List<ChartData> chartData) {
        if (chartData == null || chartData.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return chartData.stream()
                .map(ChartData::getLow)
                .min(Float::compareTo)
                .map(BigDecimal::valueOf)
                .orElse(BigDecimal.ZERO);
    }


    // OrderResponse 변환
    private OrderResponse convertToOrderResponse(Order order) {
        // Stock 정보 조회
        Stock stock = order.getStock();
        
        return new OrderResponse(
                order.getOrderId(),
                order.getStock().getId(),
                stock.getStockCode() != null ? stock.getStockCode() : "",
                stock.getStockName() != null ? stock.getStockName() : "",
                order.getOrderType().toString(),
                order.getQuantity(),
                order.getPrice(),
                order.getStatus().toString(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    // 보유 종목을 StockResponse 형식으로 조회
    @Transactional(readOnly = true)
    public List<StockResponse> getPortfolioStocks(UUID userId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        // 보유 종목 조회
        List<HoldingCache> holdings = holdingCacheRepository.findByAccountId(account.getInvestmentAccountId());
        
        return holdings.stream()
                .map(holding -> convertHoldingToStockResponse(holding))
                .collect(Collectors.toList());
    }

    // HoldingCache를 StockResponse로 변환
    private StockResponse convertHoldingToStockResponse(HoldingCache holding) {
        Stock stock = holding.getStock();
        
        // 캐시된 주식 가격 정보 조회 (일관성 보장)
        StockPriceResponse priceInfo = stockPriceService.getCachedStockPrice(stock.getId(), stock.getStockCode(), stock.getPrdtTypeCd());
        
        return new StockResponse(
                stock.getId(),
                stock.getStockCode(),
                stock.getStockName(),
                stock.getStockImage(),
                stock.getCountry().toString(),
                stock.getPrdtTypeCd(), // 주식 (300), ETF (500)
                priceInfo.getCurrentPrice().floatValue(),
                priceInfo.getChangePrice().floatValue(),
                priceInfo.getChangeRate(),
                stock.isEnabled()
        );
    }
}
