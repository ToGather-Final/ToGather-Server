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
                            .map(this::convertToChartData)
                            .sorted((a, b) -> a.getTime().compareTo(b.getTime()))
                            .collect(Collectors.toList());
                    
                    // 이동평균선 계산
                    List<ChartData> chartDataWithMA = calculateMovingAverages(rawData);
                    
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

    // 이동평균선 계산
    private List<ChartData> calculateMovingAverages(List<ChartData> rawData) {
        List<ChartData> result = new ArrayList<>();
        
        for (int i = 0; i < rawData.size(); i++) {
            ChartData current = rawData.get(i);
            
            // 5일 이동평균
            float ma5 = calculateMA(rawData, i, 5);
            
            // 20일 이동평균
            float ma20 = calculateMA(rawData, i, 20);
            
            // 60일 이동평균
            float ma60 = calculateMA(rawData, i, 60);
            
            // 120일 이동평균
            float ma120 = calculateMA(rawData, i, 120);
            
            // 새로운 ChartData 생성 (이동평균선 포함)
            ChartData chartDataWithMA = new ChartData(
                    current.getTime(),
                    current.getOpen(),
                    current.getHigh(),
                    current.getLow(),
                    current.getClose(),
                    ma5,
                    ma20,
                    ma60,
                    ma120,
                    current.getTrading_volume()
            );
            
            result.add(chartDataWithMA);
        }
        
        return result;
    }

    // 특정 기간의 이동평균 계산
    private float calculateMA(List<ChartData> data, int currentIndex, int period) {
        if (currentIndex < period - 1) {
            return 0f; // 데이터가 부족한 경우 0 반환
        }
        
        float sum = 0f;
        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            sum += data.get(i).getClose();
        }
        
        return sum / period;
    }

    // 샘플 차트 데이터 (API 실패 시 사용)
    private List<ChartData> getSampleChartData() {
        List<ChartData> sampleData = new ArrayList<>();
        
        // 30일간의 샘플 데이터 생성
        LocalDate startDate = LocalDate.now().minusDays(30);
        float basePrice = 800000f;
        
        for (int i = 0; i < 30; i++) {
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
        return calculateMovingAverages(sampleData);
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