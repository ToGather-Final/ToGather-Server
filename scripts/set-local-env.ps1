# ===============================================
# 🚀 ToGather 로컬 개발 환경변수 설정 스크립트
# ===============================================

Write-Host "🔧 로컬 개발 환경변수를 설정합니다..." -ForegroundColor Green

# Redis 설정
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:REDIS_PASSWORD = "togather1234"

# RabbitMQ 설정
$env:RABBITMQ_HOST = "localhost"
$env:RABBITMQ_PORT = "5672"
$env:RABBITMQ_USERNAME = "guest"
$env:RABBITMQ_PASSWORD = "guest"

# Database 설정 (AWS RDS 터널링)
$env:DB_URL = "jdbc:mysql://localhost:3306/togather_db"
$env:DB_USERNAME = "admin"
$env:DB_PASSWORD = "togather1234"

# JWT 설정
$env:JWT_SECRET_KEY = "TogatherSecretkey"
$env:JWT_ISSUER = "togather"
$env:JWT_ACCESS_EXP = "1800"
$env:JWT_REFRESH_EXP = "7"

# GitHub 설정
$env:GH_PAT = "CHANGE_THIS_GITHUB_PAT"

# Pay Service 설정
$env:PAY_QR_SECRET_KEY = "togather-qr-secret-key-2024"

# Spring Boot 설정
$env:SPRING_PROFILES_ACTIVE = "dev"

Write-Host "✅ 환경변수 설정 완료!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 설정된 환경변수:" -ForegroundColor Cyan
Write-Host "  - REDIS_HOST: $env:REDIS_HOST" -ForegroundColor White
Write-Host "  - REDIS_PASSWORD: $env:REDIS_PASSWORD" -ForegroundColor White
Write-Host "  - RABBITMQ_HOST: $env:RABBITMQ_HOST" -ForegroundColor White
Write-Host "  - RABBITMQ_USERNAME: $env:RABBITMQ_USERNAME" -ForegroundColor White
Write-Host "  - DB_URL: $env:DB_URL" -ForegroundColor White
Write-Host "  - JWT_SECRET_KEY: $env:JWT_SECRET_KEY" -ForegroundColor White
Write-Host "  - SPRING_PROFILES_ACTIVE: $env:SPRING_PROFILES_ACTIVE" -ForegroundColor White
Write-Host ""
Write-Host "🚀 이제 Spring Boot 애플리케이션을 실행할 수 있습니다!" -ForegroundColor Yellow
