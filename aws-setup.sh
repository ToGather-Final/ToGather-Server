#!/bin/bash

# AWS ECR 및 EKS 설정 스크립트

# 변수 설정
AWS_REGION="ap-northeast-2"
ECR_REGISTRY="your-account-id.dkr.ecr.ap-northeast-2.amazonaws.com"
EKS_CLUSTER_NAME="togather-cluster"
EKS_NODE_GROUP_NAME="togather-nodes"

echo "🚀 AWS ECR 및 EKS 설정을 시작합니다..."

# 1. ECR 리포지토리 생성
echo "📦 ECR 리포지토리 생성 중..."
aws ecr create-repository --repository-name togather/api-gateway --region $AWS_REGION
aws ecr create-repository --repository-name togather/user-service --region $AWS_REGION
aws ecr create-repository --repository-name togather/trading-service --region $AWS_REGION
aws ecr create-repository --repository-name togather/pay-service --region $AWS_REGION
aws ecr create-repository --repository-name togather/vote-service --region $AWS_REGION

# 2. EKS 클러스터 생성
echo "☸️ EKS 클러스터 생성 중..."
eksctl create cluster \
    --name $EKS_CLUSTER_NAME \
    --region $AWS_REGION \
    --nodegroup-name $EKS_NODE_GROUP_NAME \
    --node-type t3.medium \
    --nodes 3 \
    --nodes-min 2 \
    --nodes-max 5 \
    --managed

# 3. kubectl 설정
echo "🔧 kubectl 설정 중..."
aws eks update-kubeconfig --region $AWS_REGION --name $EKS_CLUSTER_NAME

# 4. AWS Load Balancer Controller 설치
echo "⚖️ AWS Load Balancer Controller 설치 중..."
kubectl apply -k "github.com/aws/eks-charts/stable/aws-load-balancer-controller/crds?ref=master"
helm repo add eks https://aws.github.io/eks-charts
helm repo update
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
    -n kube-system \
    --set clusterName=$EKS_CLUSTER_NAME

# 5. Metrics Server 설치 (HPA용)
echo "📊 Metrics Server 설치 중..."
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

echo "✅ AWS 설정이 완료되었습니다!"
echo ""
echo "📋 다음 단계:"
echo "1. GitHub Secrets에 다음 값들을 설정하세요:"
echo "   - AWS_ACCESS_KEY_ID"
echo "   - AWS_SECRET_ACCESS_KEY"
echo "2. k8s/configmap.yaml에서 실제 RDS 엔드포인트로 수정하세요"
echo "3. k8s/secret.yaml에서 실제 비밀번호로 수정하세요"
echo "4. .github/workflows/deploy.yml에서 ECR_REGISTRY를 실제 값으로 수정하세요"
echo "5. git push origin main으로 배포를 시작하세요"
