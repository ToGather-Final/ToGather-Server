# ===============================================
# 🚀 ToGather 로컬 개발 환경 시작 스크립트 (PowerShell)
# ===============================================

Write-Host "🚀 ToGather 로컬 개발 환경을 시작합니다..." -ForegroundColor Green

# .env.local 파일 확인
if (-not (Test-Path ".env.local")) {
    Write-Host "❌ .env.local 파일을 찾을 수 없습니다." -ForegroundColor Red
    Write-Host "📋 env.local.example 파일을 .env.local로 복사하고 값을 수정하세요:" -ForegroundColor Yellow
    Write-Host "   Copy-Item env.local.example .env.local" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "🔧 또는 다음 환경변수를 직접 설정하세요:" -ForegroundColor Cyan
    Write-Host "   \$env:REDIS_HOST='localhost'" -ForegroundColor White
    Write-Host "   \$env:REDIS_PASSWORD='togather1234'" -ForegroundColor White
    Write-Host "   \$env:JWT_SECRET_KEY='TogatherSecretkey'" -ForegroundColor White
    Write-Host "   \$env:DB_URL='jdbc:mysql://localhost:3306/togather_db'" -ForegroundColor White
    Write-Host "   \$env:DB_USERNAME='admin'" -ForegroundColor White
    Write-Host "   \$env:DB_PASSWORD='togather1234'" -ForegroundColor White
    exit 1
}

# Docker Compose로 Redis와 RabbitMQ 시작
Write-Host "🐳 Docker Compose로 Redis와 RabbitMQ를 시작하는 중..." -ForegroundColor Blue
docker-compose -f docker-compose.local.yml up -d

# 서비스 상태 확인
Write-Host "⏳ 서비스가 시작될 때까지 잠시 기다리는 중..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Redis 연결 테스트
Write-Host "🔍 Redis 연결 테스트..." -ForegroundColor Blue
$redisTest = docker exec togather-redis-local redis-cli --no-auth-warning -a togather1234 ping
if ($redisTest -match "PONG") {
    Write-Host "✅ Redis 연결 성공!" -ForegroundColor Green
} else {
    Write-Host "❌ Redis 연결 실패!" -ForegroundColor Red
    exit 1
}

# RabbitMQ 연결 테스트
Write-Host "🔍 RabbitMQ 연결 테스트..." -ForegroundColor Blue
try {
    $response = Invoke-WebRequest -Uri "http://localhost:15672/api/overview" -Credential (New-Object System.Management.Automation.PSCredential("guest", (ConvertTo-SecureString "guest" -AsPlainText -Force))) -UseBasicParsing
    if ($response.StatusCode -eq 200) {
        Write-Host "✅ RabbitMQ 연결 성공!" -ForegroundColor Green
        Write-Host "🌐 RabbitMQ Management UI: http://localhost:15672 (guest/guest)" -ForegroundColor Cyan
    }
} catch {
    Write-Host "❌ RabbitMQ 연결 실패!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "🎉 로컬 개발 환경이 준비되었습니다!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 사용 가능한 서비스:" -ForegroundColor Cyan
Write-Host "  - Redis: localhost:6379 (비밀번호: togather1234)" -ForegroundColor White
Write-Host "  - RabbitMQ: localhost:5672 (guest/guest)" -ForegroundColor White
Write-Host "  - RabbitMQ Management: http://localhost:15672" -ForegroundColor White
Write-Host ""
Write-Host "🚀 이제 Spring Boot 애플리케이션을 실행할 수 있습니다:" -ForegroundColor Yellow
Write-Host "  .\gradlew.bat :user-service:bootRun" -ForegroundColor White
Write-Host "  .\gradlew.bat :trading-service:bootRun" -ForegroundColor White
Write-Host "  .\gradlew.bat :pay-service:bootRun" -ForegroundColor White
Write-Host "  .\gradlew.bat :vote-service:bootRun" -ForegroundColor White
Write-Host "  .\gradlew.bat :api-gateway:bootRun" -ForegroundColor White
