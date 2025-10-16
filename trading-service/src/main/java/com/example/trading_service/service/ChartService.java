package com.example.trading_service.service;

import com.example.trading_service.dto.ChartData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChartService {

    private final StockPriceService stockPriceService;

    // 주식 차트 데이터 조회 (이동평균선 포함)
    public List<ChartData> getStockChart(String stockCode, int days) {
        try {
            Map<String, Object> chartResponse = stockPriceService.getStockChart(stockCode, "D");
            
            if (chartResponse != null && chartResponse.containsKey("output2")) {
                List<Map<String, Object>> chartList = (List<Map<String, Object>>) chartResponse.get("output2");
                
                if (chartList != null && !chartList.isEmpty()) {
                    // 원본 데이터를 날짜 순으로 정렬 (오래된 것부터)
                    List<ChartData> rawData = chartList.stream()
                            .map(item -> convertToChartData(item))
                            .sorted((a, b) -> a.getTime().compareTo(b.getTime()))
                            .collect(Collectors.toList());
                    
                    // 이동평균선 계산
                    List<ChartData> chartDataWithMA = calculateMovingAverages(rawData, "D");
                    
                    // 최근 데이터만 반환 (지정된 기간만큼)
                    return chartDataWithMA.stream()
                            .skip(Math.max(0, chartDataWithMA.size() - days))
                            .collect(Collectors.toList());
                }
            }
            
            log.warn("차트 데이터를 가져올 수 없습니다. 종목코드: {}", stockCode);
            return getSampleChartData();
            
        } catch (Exception e) {
            log.error("차트 데이터 조회 중 오류 발생. 종목코드: {}, 오류: {}", stockCode, e.getMessage());
            return getSampleChartData();
        }
    }

    // 주식 차트 데이터 조회 (기간분류코드 사용)
    public List<ChartData> getStockChartByPeriod(String stockCode, String periodDiv) {
        try {
            Map<String, Object> chartResponse = stockPriceService.getStockChart(stockCode, periodDiv);
            
            if (chartResponse != null && chartResponse.containsKey("output2")) {
                List<Map<String, Object>> chartList = (List<Map<String, Object>>) chartResponse.get("output2");
                
                if (chartList != null && !chartList.isEmpty()) {
                    // 원본 데이터를 날짜 순으로 정렬 (오래된 것부터)
                    List<ChartData> rawData = chartList.stream()
                            .map(item -> convertToChartData(item))
                            .sorted((a, b) -> a.getTime().compareTo(b.getTime()))
                            .collect(Collectors.toList());
                    
                    // 이동평균선 계산
                    List<ChartData> chartDataWithMA = calculateMovingAverages(rawData, periodDiv);
                    
                    return chartDataWithMA;
                }
            }
            
            log.warn("차트 데이터를 가져올 수 없습니다. 종목코드: {}, 기간분류코드: {}", stockCode, periodDiv);
            return getSampleChartData();
            
        } catch (Exception e) {
            log.error("차트 데이터 조회 중 오류 발생. 종목코드: {}, 기간분류코드: {}, 오류: {}", stockCode, periodDiv, e.getMessage());
            return getSampleChartData();
        }
    }

    // 한투 API 차트 데이터를 ChartData로 변환
    private ChartData convertToChartData(Map<String, Object> chartItem) {
        String time = (String) chartItem.get("stck_bsop_date"); // 날짜
        float open = parseFloat(chartItem.get("stck_oprc")); // 시가
        float high = parseFloat(chartItem.get("stck_hgpr")); // 고가
        float low = parseFloat(chartItem.get("stck_lwpr")); // 저가
        float close = parseFloat(chartItem.get("stck_clpr")); // 종가
        long trading_volume = parseLong(chartItem.get("acml_vol")); // 거래량
        
        // 이동평균선은 나중에 계산하므로 0으로 초기화
        return new ChartData(time, open, high, low, close, 0f, 0f, 0f, 0f, trading_volume);
    }

    // 이동평균선 계산 (기간분류코드별)
    private List<ChartData> calculateMovingAverages(List<ChartData> rawData, String periodDiv) {
        List<ChartData> result = new ArrayList<>();
        
        // 기간분류코드별 이동평균 기간 설정
        int[] maPeriods = getMovingAveragePeriods(periodDiv);
        
        log.info("이동평균선 계산 시작 - periodDiv: {}, 데이터 크기: {}, 이동평균 기간: [{}, {}, {}, {}]", 
                periodDiv, rawData.size(), maPeriods[0], maPeriods[1], maPeriods[2], maPeriods[3]);
        
        // 첫 번째와 마지막 데이터의 종가 로그
        if (!rawData.isEmpty()) {
            log.info("첫 번째 데이터: 날짜={}, 종가={}", rawData.get(0).getTime(), rawData.get(0).getClose());
            log.info("마지막 데이터: 날짜={}, 종가={}", rawData.get(rawData.size()-1).getTime(), rawData.get(rawData.size()-1).getClose());
        }
        
        for (int i = 0; i < rawData.size(); i++) {
            ChartData current = rawData.get(i);
            
            // 각 기간별 이동평균 계산
            float ma1 = calculateMA(rawData, i, maPeriods[0]);
            float ma2 = calculateMA(rawData, i, maPeriods[1]);
            float ma3 = calculateMA(rawData, i, maPeriods[2]);
            float ma4 = calculateMA(rawData, i, maPeriods[3]);
            
            // 새로운 ChartData 생성 (이동평균선 포함)
            ChartData chartDataWithMA = new ChartData(
                    current.getTime(),
                    current.getOpen(),
                    current.getHigh(),
                    current.getLow(),
                    current.getClose(),
                    ma1,  // ma_5
                    ma2,  // ma_10
                    ma3,  // ma_20
                    ma4,  // ma_60
                    current.getTrading_volume()
            );
            
            result.add(chartDataWithMA);
        }
        
        log.info("이동평균선 계산 완료 - 결과 데이터 크기: {}", result.size());
        
        // 계산 결과 샘플 로그 (마지막 3개 데이터)
        if (result.size() >= 3) {
            for (int i = result.size() - 3; i < result.size(); i++) {
                ChartData data = result.get(i);
                log.info("결과 데이터[{}]: 날짜={}, 종가={}, MA5={}, MA10={}, MA20={}, MA60={}", 
                        i, data.getTime(), data.getClose(), data.getMa_5(), data.getMa_10(), 
                        data.getMa_20(), data.getMa_60());
            }
        }
        
        return result;
    }
    
    // 기간분류코드별 이동평균 기간 설정
    private int[] getMovingAveragePeriods(String periodDiv) {
        switch (periodDiv.toUpperCase()) {
            case "D": // 일봉: 5일, 10일, 20일, 60일 (더 현실적인 기간)
                return new int[]{5, 10, 20, 60};
            case "W": // 주봉: 4주, 8주, 12주, 24주
                return new int[]{4, 8, 12, 24};
            case "M": // 월봉: 3개월, 6개월, 12개월, 24개월
                return new int[]{3, 6, 12, 24};
            case "Y": // 연봉: 2년, 3년, 5년, 10년
                return new int[]{2, 3, 5, 10};
            default: // 기본값 (일봉)
                return new int[]{5, 10, 20, 60};
        }
    }

    // 특정 기간의 이동평균 계산
    private float calculateMA(List<ChartData> data, int currentIndex, int period) {
        // 요청된 기간의 데이터가 충분하지 않으면 0 반환
        if (currentIndex < period - 1) {
            return 0f;
        }
        
        // 계산 시작 인덱스 (정확히 period 개의 데이터 사용)
        int startIndex = currentIndex - period + 1;
        
        float sum = 0f;
        for (int i = startIndex; i <= currentIndex; i++) {
            sum += data.get(i).getClose();
        }
        
        float result = sum / period;
        
        // 디버그 로그 (처음 몇 개만)
        if (currentIndex < 10) {
            log.info("이동평균 계산: period: {}, currentIndex: {}, startIndex: {}, sum: {}, result: {}", 
                     period, currentIndex, startIndex, sum, result);
        }
        
        return result;
    }

    // 샘플 차트 데이터 (API 실패 시 사용)
    private List<ChartData> getSampleChartData() {
        List<ChartData> sampleData = new ArrayList<>();
        
        // 90일간의 샘플 데이터 생성 (이동평균 계산을 위해 충분한 데이터)
        LocalDate startDate = LocalDate.now().minusDays(90);
        float basePrice = 800000f;
        
        for (int i = 0; i < 90; i++) {
            LocalDate date = startDate.plusDays(i);
            String time = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // 랜덤한 가격 변동 생성
            float variation = (float) (Math.random() - 0.5) * 20000; // ±10,000원 변동
            float open = basePrice + variation;
            float close = open + (float) (Math.random() - 0.5) * 10000;
            float high = Math.max(open, close) + (float) Math.random() * 5000;
            float low = Math.min(open, close) - (float) Math.random() * 5000;
            long volume = (long) (Math.random() * 1000000) + 500000; // 50만~150만 거래량
            
            sampleData.add(new ChartData(time, open, high, low, close, 0f, 0f, 0f, 0f, volume));
            basePrice = close; // 다음 날의 기준가격
        }
        
        // 이동평균선 계산
        return calculateMovingAverages(sampleData, "D");
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
}