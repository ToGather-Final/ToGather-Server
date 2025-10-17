# ToGather 성능 테스트 스크립트
# 사용법: .\scripts\performance-test.ps1

param(
    [string]$Url = "https://xn--o79aq2k062a.store",
    [int]$TestCount = 10
)

Write-Host "🚀 ToGather 성능 테스트 시작..." -ForegroundColor Green
Write-Host "테스트 URL: $Url" -ForegroundColor Cyan
Write-Host "테스트 횟수: $TestCount" -ForegroundColor Cyan
Write-Host ""

$results = @()

for ($i = 1; $i -le $TestCount; $i++) {
    Write-Host "테스트 $i/$TestCount 진행 중..." -NoNewline
    
    $measurement = Measure-Command { 
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 30
            $statusCode = $response.StatusCode
        } catch {
            $statusCode = "ERROR"
        }
    }
    
    $totalMs = [math]::Round($measurement.TotalMilliseconds, 2)
    $results += $totalMs
    
    if ($statusCode -eq 200) {
        Write-Host " ✅ ${totalMs}ms" -ForegroundColor Green
    } else {
        Write-Host " ❌ ${totalMs}ms (Status: $statusCode)" -ForegroundColor Red
    }
    
    Start-Sleep -Milliseconds 500
}

# 통계 계산
$avg = [math]::Round(($results | Measure-Object -Average).Average, 2)
$min = [math]::Round(($results | Measure-Object -Minimum).Minimum, 2)
$max = [math]::Round(($results | Measure-Object -Maximum).Maximum, 2)

Write-Host ""
Write-Host "📊 성능 테스트 결과" -ForegroundColor Yellow
Write-Host "==================" -ForegroundColor Yellow
Write-Host "평균 응답시간: $avg ms" -ForegroundColor White
Write-Host "최소 응답시간: $min ms" -ForegroundColor Green
Write-Host "최대 응답시간: $max ms" -ForegroundColor Red

# 성능 등급 평가
if ($avg -lt 1000) {
    Write-Host "등급: 🟢 우수 (1초 미만)" -ForegroundColor Green
} elseif ($avg -lt 2000) {
    Write-Host "등급: 🟡 보통 (1-2초)" -ForegroundColor Yellow
} elseif ($avg -lt 3000) {
    Write-Host "등급: 🟠 개선 필요 (2-3초)" -ForegroundColor DarkYellow
} else {
    Write-Host "등급: 🔴 개선 시급 (3초 이상)" -ForegroundColor Red
}

Write-Host ""
Write-Host "💡 성능 개선 권장사항:" -ForegroundColor Cyan
Write-Host "1. CDN 사용 고려" -ForegroundColor White
Write-Host "2. 이미지 최적화" -ForegroundColor White
Write-Host "3. 코드 스플리팅 적용" -ForegroundColor White
Write-Host "4. 캐싱 전략 개선" -ForegroundColor White
Write-Host "5. 데이터베이스 쿼리 최적화" -ForegroundColor White
