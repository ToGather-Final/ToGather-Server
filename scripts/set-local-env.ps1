# ===============================================
# 🚀 ToGather 로컬 개발 환경변수 설정 스크립트
# ===============================================

Write-Host "🔧 로컬 개발 환경변수를 설정합니다..." -ForegroundColor Green

# .env.dev 파일이 있으면 읽어서 환경변수 설정
if (Test-Path ".env.dev") {
    Write-Host "📁 .env.dev 파일을 읽어서 환경변수를 설정합니다..." -ForegroundColor Blue
    Get-Content ".env.dev" | ForEach-Object {
        if ($_ -match '^([^#][^=]+)=(.*)$') {
            [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
        }
    }
    Write-Host "✅ .env.dev 파일에서 환경변수 로드 완료!" -ForegroundColor Green
} else {
    Write-Host "⚠️ .env.dev 파일을 찾을 수 없습니다. 기본값을 사용합니다." -ForegroundColor Yellow
    
    # 기본값 설정 (fallback)
    $env:REDIS_HOST = "localhost"
    $env:REDIS_PORT = "6379"
    $env:REDIS_PASSWORD = "togather1234"
    $env:RABBITMQ_HOST = "localhost"
    $env:RABBITMQ_PORT = "5672"
    $env:RABBITMQ_USERNAME = "guest"
    $env:RABBITMQ_PASSWORD = "guest"
    $env:DB_URL = "jdbc:mysql://localhost:3307/togather_db"
    $env:DB_USERNAME = "admin"
    $env:DB_PASSWORD = "togather1234"
    $env:JWT_SECRET_KEY = "k8vJ2mN9pQ3rS7tU1wX5yZ8aB4cD6eF9gH2jK5lM8nP1qR4sT7uV0wX3yZ6"
    $env:JWT_ISSUER = "togather"
    $env:JWT_ACCESS_EXP = "1800"
    $env:JWT_REFRESH_EXP = "7"
    $env:GH_PAT = "CHANGE_THIS_GITHUB_PAT"
    $env:PAY_QR_SECRET_KEY = "togather-qr-secret-key-2024"
}

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
