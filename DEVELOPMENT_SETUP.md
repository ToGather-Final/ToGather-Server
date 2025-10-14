# 🚀 ToGather 개발환경 설정 가이드

## 📋 개요
이 가이드는 ToGather 프로젝트의 로컬 개발환경을 설정하는 방법을 설명합니다.

## 🔧 개발환경 설정

### 1️⃣ 필수 파일 다운로드
팀장 또는 기존 개발자로부터 다음 파일을 받아서 프로젝트 루트에 배치하세요:

```
ToGather-Server/
├── application-dev.yml  # ← 이 파일을 받아서 루트에 넣으세요
└── ...
```

### 2️⃣ Spring Boot 애플리케이션 실행
```bash
# API Gateway 실행
cd api-gateway
./gradlew bootRun

# User Service 실행 (새 터미널)
cd user-service
./gradlew bootRun

# Trading Service 실행 (새 터미널)
cd trading-service
./gradlew bootRun

# Pay Service 실행 (새 터미널)
cd pay-service
./gradlew bootRun

# Vote Service 실행 (새 터미널)
cd vote-service
./gradlew bootRun
```

### 3️⃣ 서비스 포트 확인
- **API Gateway**: http://localhost:8000
- **User Service**: http://localhost:8082 (dev)
- **Trading Service**: http://localhost:8081 (dev)
- **Pay Service**: http://localhost:8083 (dev)
- **Vote Service**: http://localhost:8080 (dev)

## 🔒 보안 주의사항

### ⚠️ 중요!
- `application-dev.yml` 파일은 **절대 Git에 커밋하지 마세요**
- 이 파일에는 민감한 정보(비밀번호, API 키 등)가 포함되어 있습니다
- 파일이 이미 `.gitignore`에 추가되어 있어 자동으로 제외됩니다

### 🔄 환경변수 변경 시
1. `application-dev.yml` 파일 수정
2. Spring Boot 애플리케이션 재시작
3. 변경사항이 자동으로 적용됩니다

## 🐳 Docker Compose 사용 (선택사항)

### Redis와 RabbitMQ 실행
```bash
# Docker Compose로 인프라 서비스 실행
docker-compose up -d rabbitmq redis
```

### 전체 서비스 실행
```bash
# 모든 서비스를 Docker Compose로 실행
docker-compose up
```

## 🛠️ 문제 해결

### 환경변수가 적용되지 않는 경우
1. `application-dev.yml` 파일이 프로젝트 루트에 있는지 확인
2. Spring Boot 애플리케이션을 완전히 재시작
3. 로그에서 환경변수 로드 확인

### 포트 충돌 시
- 각 서비스는 다른 포트를 사용합니다
- 포트가 사용 중이면 해당 프로세스를 종료하세요

## 📞 지원
문제가 발생하면 팀 채널에서 문의하세요!

---
**Happy Coding! 🎉**
