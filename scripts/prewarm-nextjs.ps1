# Next.js 클라이언트 Prewarming 스크립트
# Pod 재시작 후 자동으로 워밍업하여 콜드 스타트 제거

param(
    [string]$Url = "https://xn--o79aq2k062a.store",
    [int]$WarmupRequests = 5
)

Write-Host "Next.js Prewarming 시작..." -ForegroundColor Cyan
Write-Host "URL: $Url" -ForegroundColor White
Write-Host "워밍업 요청 수: $WarmupRequests" -ForegroundColor White
Write-Host ""

for ($i = 1; $i -le $WarmupRequests; $i++) {
    Write-Host "워밍업 요청 $i/$WarmupRequests..." -NoNewline
    
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 30
        if ($response.StatusCode -eq 200) {
            Write-Host " OK" -ForegroundColor Green
        } else {
            Write-Host " WARNING (Status: $($response.StatusCode))" -ForegroundColor Yellow
        }
    } catch {
        Write-Host " ERROR" -ForegroundColor Red
    }
    
    Start-Sleep -Milliseconds 500
}

Write-Host ""
Write-Host "Prewarming 완료! Next.js 클라이언트가 준비되었습니다." -ForegroundColor Green
Write-Host ""
Write-Host "이제 성능 테스트를 실행하세요:" -ForegroundColor Cyan
Write-Host "  .\scripts\performance-test.ps1" -ForegroundColor White

