@echo off
REM

setlocal enabledelayedexpansion

REM 파라미터 설정 deploy-api-gateway.bat
set IMAGE_TAG=latest
if not "%~1"=="" set IMAGE_TAG=%~1

REM ECR 레지스트리 정보
set ECR_REGISTRY=340623336075.dkr.ecr.ap-northeast-2.amazonaws.com
set IMAGE_NAME=togather/api-gateway
set FULL_IMAGE_NAME=%ECR_REGISTRY%/%IMAGE_NAME%:%IMAGE_TAG%

echo.
echo ===============================================
echo 🚀 API Gateway 배포 시작...
echo ===============================================
echo 이미지 태그: %IMAGE_TAG%
echo 전체 이미지명: %FULL_IMAGE_NAME%
echo.

REM 1단계: Java 환경변수 설정
echo 1️⃣ Java 환경변수 설정 중...
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.8.7-hotspot
if not exist "%JAVA_HOME%" (
    echo ❌ Java 17이 설치되지 않았습니다: %JAVA_HOME%
    pause
    exit /b 1
)
echo ✅ Java 환경변수 설정 완료

REM 2단계: API Gateway 빌드
echo.
echo 2️⃣ API Gateway 빌드 중...
call gradlew :api-gateway:bootJar --no-daemon
if errorlevel 1 (
    echo ❌ 빌드 실패
    pause
    exit /b 1
)
echo ✅ 빌드 완료

REM 3단계: Docker 이미지 빌드
echo.
echo 3️⃣ Docker 이미지 빌드 중...
docker build -f api-gateway/Dockerfile -t %FULL_IMAGE_NAME% .
if errorlevel 1 (
    echo ❌ Docker 이미지 빌드 실패
    pause
    exit /b 1
)
echo ✅ Docker 이미지 빌드 완료

REM 4단계: ECR 로그인
echo.
echo 4️⃣ AWS ECR 로그인 중...
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin %ECR_REGISTRY%
if errorlevel 1 (
    echo ❌ ECR 로그인 실패
    pause
    exit /b 1
)
echo ✅ ECR 로그인 완료

REM 5단계: 이미지 푸시
echo.
echo 5️⃣ Docker 이미지 푸시 중...
docker push %FULL_IMAGE_NAME%
if errorlevel 1 (
    echo ❌ 이미지 푸시 실패
    pause
    exit /b 1
)
echo ✅ 이미지 푸시 완료

REM 6단계: EKS 클러스터 연결
echo.
echo 6️⃣ EKS 클러스터 연결 중...
aws eks update-kubeconfig --region ap-northeast-2 --name togather-cluster
if errorlevel 1 (
    echo ❌ EKS 클러스터 연결 실패
    pause
    exit /b 1
)
echo ✅ EKS 클러스터 연결 완료

REM 7단계: Kubernetes 배포
echo.
echo 7️⃣ Kubernetes 배포 중...
echo    기존 API Gateway 배포 삭제 중...
kubectl delete deployment api-gateway -n togather --ignore-not-found=true

echo    새 API Gateway 배포 적용 중...
kubectl apply -f k8s/api-gateway.yaml -n togather
if errorlevel 1 (
    echo ❌ Kubernetes 배포 실패
    pause
    exit /b 1
)
echo ✅ Kubernetes 배포 완료

REM 8단계: 배포 상태 확인
echo.
echo 8️⃣ 배포 상태 확인 중...
echo    롤아웃 상태 확인...
kubectl rollout status deployment api-gateway -n togather --timeout=300s
if errorlevel 1 (
    echo ⚠️ 롤아웃 상태 확인 실패 (타임아웃 또는 오류)
)

REM 9단계: 파드 상태 및 로그 확인
echo.
echo 9️⃣ 파드 상태 확인 중...
echo    파드 목록:
kubectl get pods -n togather -l app=api-gateway

echo.
echo    최근 로그 (마지막 20줄):
kubectl logs -n togather -l app=api-gateway --tail=20

echo.
echo ===============================================
echo 🎉 API Gateway 배포 완료!
echo ===============================================
echo.
echo 📋 유용한 명령어:
echo    로그 확인: kubectl logs -n togather -l app=api-gateway -f
echo    파드 상태: kubectl get pods -n togather -l app=api-gateway
echo    서비스 상태: kubectl get svc -n togather api-gateway
echo    포트 포워딩: kubectl port-forward -n togather service/api-gateway 8000:8000
echo.

pause
