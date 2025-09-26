#!/bin/bash

# ToGather Kubernetes 리소스 삭제 스크립트

echo "🗑️ ToGather 마이크로서비스 삭제를 시작합니다..."

# 모든 리소스 삭제
kubectl delete -f hpa.yaml
kubectl delete -f ingress.yaml
kubectl delete -f nginx.yaml
kubectl delete -f api-gateway.yaml
kubectl delete -f vote-service.yaml
kubectl delete -f pay-service.yaml
kubectl delete -f trading-service.yaml
kubectl delete -f user-service.yaml
kubectl delete -f rabbitmq.yaml
kubectl delete -f secret.yaml
kubectl delete -f configmap.yaml
kubectl delete -f namespace.yaml

echo "✅ 모든 리소스가 삭제되었습니다!"
echo ""
echo "📊 남은 리소스 확인:"
kubectl get all -n togather
