# =================================================================
# 1단계: 빌드 환경 (Builder Stage)
# - 소스 코드를 받아 실행 가능한 JAR 파일로 빌드하는 역할만 수행
# - JDK와 Gradle이 설치된 이미지를 사용
# =================================================================
FROM gradle:8.9-jdk17-alpine AS builder

# 작업 디렉토리 설정
WORKDIR /build

# 전체 프로젝트를 복사하여 빌드 컨텍스트로 사용
# 이렇게 해야 루트 프로젝트의 settings.gradle을 인식하여 멀티모듈 빌드가 가능
COPY . .

# --no-daemon 옵션은 CI/CD 환경에서 빌드 후 프로세스가 즉시 종료되도록 보장
RUN gradle :trading-service:bootJar --no-daemon


# =================================================================
# 2단계: 실행 환경 (Final Stage)
# - 1단계에서 빌드된 JAR 파일만 가져와 실행하는 역할
# - JRE만 포함된 가벼운 이미지를 사용해 최종 이미지 크기를 최소화
# =================================================================
FROM eclipse-temurin:17-jre-alpine

# 작업 디렉토리 설정
WORKDIR /app

# 보안을 위해 nobody 유저를 사용 (root 사용 방지)
USER nobody

# 1단계(builder)에서 빌드된 결과물(JAR 파일)을 복사
COPY --from=builder /build/trading-service/build/libs/*.jar app.jar

# 애플리케이션 실행
# Spring Boot의 기본 포트인 8080을 노출
EXPOSE 8080

# 최종 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]