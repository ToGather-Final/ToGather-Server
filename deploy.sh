#!/bin/bash

# ToGather 서비스 배포 스크립트
echo "🚀 ToGather 서비스 배포 시작..."

# 1. 네임스페이스 삭제/생성
echo "📦 네임스페이스 관리..."
kubectl delete namespace togather --ignore-not-found=true
kubectl create namespace togather

# 2. Secret 생성
echo "🔐 Secret 생성..."
kubectl create secret generic togather-secrets \
  --from-literal=SPRING_DATASOURCE_USERNAME="admin" \
  --from-literal=SPRING_DATASOURCE_PASSWORD="togather1234" \
  --from-literal=DB_USERNAME="admin" \
  --from-literal=DB_PASSWORD="togather1234" \
  --from-literal=JWT_SECRET_KEY="TogatherSecretkey" \
  --from-literal=JWT_SECRET="TogatherSecretkey" \
  --from-literal=SPRING_RABBITMQ_USERNAME="admin" \
  --from-literal=SPRING_RABBITMQ_PASSWORD="togather1234" \
  --from-literal=RABBITMQ_PASSWORD="togather1234" \
  --from-literal=REDIS_PASSWORD="togather1234" \
  --from-literal=SPRING_DATA_REDIS_PASSWORD="togather1234" \
  -n togather

# 3. API Gateway 빌드 및 이미지 생성
echo "🔨 API Gateway 빌드..."
./gradlew :api-gateway:build -x test

echo "🐳 API Gateway Docker 이미지 빌드..."
cd api-gateway
docker build -t 340623336075.dkr.ecr.ap-northeast-2.amazonaws.com/togather/api-gateway:latest .

echo "📤 ECR에 이미지 푸시..."
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin 340623336075.dkr.ecr.ap-northeast-2.amazonaws.com
docker push 340623336075.dkr.ecr.ap-northeast-2.amazonaws.com/togather/api-gateway:latest

cd ..

# 4. 다른 서비스들 빌드 및 이미지 생성
echo "🔨 다른 서비스들 빌드..."

# User Service
echo "👤 User Service 빌드..."
./gradlew :user-service:build -x test
cd user-service
docker build -t 340623336075.dkr.ecr.ap-northeast-2.amazonaws.com/togather/user-service:latest .
docker push 340623336075.dkr.ecr.ap-northeast-2.amazonaws.com/togather/user-service:latest
cd ..

# Vote Service
echo "🗳️ Vote Service 빌드..."
./gradlew :vote-service:build -x test
cd vote-service
docker build -t 340623336075.dkr.ecr.ap-northeast-2.amazonaws.com/togather/vote-service:latest .
docker push 340623336075.dkr.ecr.ap-northeast-2.amazonaws.com/togather/vote-service:latest
cd ..

# Trading Service
echo "💰 Trading Service 빌드..."
./gradlew :trading-service:build -x test
cd trading-service
docker build -t 340623336075.dkr.ecr.ap-northeast-2.amazonaws.com/togather/trading-service:latest .
docker push 340623336075.dkr.ecr.ap-northeast-2.amazonaws.com/togather/trading-service:latest
cd ..

# Pay Service
echo "💳 Pay Service 빌드..."
./gradlew :pay-service:build -x test
cd pay-service
docker build -t 340623336075.dkr.ecr.ap-northeast-2.amazonaws.com/togather/pay-service:latest .
docker push 340623336075.dkr.ecr.ap-northeast-2.amazonaws.com/togather/pay-service:latest
cd ..

# 5. Kubernetes 배포
echo "☸️ Kubernetes 배포..."
kubectl apply -f k8s/ --namespace=togather

echo "✅ 배포 완료!"
echo "📊 Pod 상태 확인:"
kubectl get pods -n togather

echo "📋 서비스 상태 확인:"
kubectl get services -n togather
