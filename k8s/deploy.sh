#!/bin/bash

# ToGather Kubernetes 배포 스크립트

echo "🚀 ToGather 마이크로서비스 배포를 시작합니다..."

# 1. Namespace 생성
echo "📦 Namespace 생성 중..."
kubectl apply -f namespace.yaml

# 2. ConfigMap 생성 (비민감 데이터만)
echo "🔧 ConfigMap 생성 중..."
kubectl apply -f configmap.yaml
kubectl apply -f configmap-env.yaml

# 3. Secret 생성 (GitHub Secrets 사용 - 로컬에서는 수동 설정 필요)
echo "🔐 Secret 생성 중..."
echo "⚠️  로컬 환경에서는 수동으로 Secret을 생성해야 합니다:"
echo "kubectl create secret generic togather-secret \\"
echo "  --from-literal=DB_PASSWORD='your-password' \\"
echo "  --from-literal=JWT_SECRET_KEY='your-jwt-secret' \\"
echo "  --from-literal=RABBITMQ_PASSWORD='guest' \\"
echo "  --namespace=togather"

# 4. 보안 정책 적용
echo "🔒 보안 정책 적용 중..."
kubectl apply -f security-policy.yaml

# 5. RabbitMQ 배포
echo "🐰 RabbitMQ 배포 중..."
kubectl apply -f rabbitmq.yaml

# 6. 마이크로서비스 배포
echo "🔧 마이크로서비스 배포 중..."
kubectl apply -f user-service.yaml
kubectl apply -f trading-service.yaml
kubectl apply -f pay-service.yaml
kubectl apply -f vote-service.yaml

# 7. API Gateway 배포
echo "🌐 API Gateway 배포 중..."
kubectl apply -f api-gateway.yaml

# 8. Nginx 배포
echo "🔀 Nginx 배포 중..."
kubectl apply -f nginx.yaml

# 9. Ingress 설정
echo "🌍 Ingress 설정 중..."
kubectl apply -f ingress.yaml

# 10. HPA 설정
echo "📈 HPA 설정 중..."
kubectl apply -f hpa.yaml

echo "✅ 배포가 완료되었습니다!"
echo ""
echo "📊 배포 상태 확인:"
kubectl get pods -n togather
echo ""
echo "🌐 서비스 상태 확인:"
kubectl get services -n togather
echo ""
echo "🔍 Ingress 상태 확인:"
kubectl get ingress -n togather
