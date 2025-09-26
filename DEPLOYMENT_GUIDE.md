# ToGather 마이크로서비스 배포 가이드

## 🏗️ 아키텍처 개요

이 프로젝트는 Spring Boot 기반의 마이크로서비스 아키텍처로 구성되어 있으며, 다음과 같은 기술 스택을 사용합니다:

- **Backend**: Spring Boot 3.5.6, Java 17
- **Database**: MySQL (AWS RDS)
- **Message Queue**: RabbitMQ
- **API Gateway**: Spring Boot + JWT 인증
- **Reverse Proxy**: Nginx
- **Container**: Docker
- **Orchestration**: Kubernetes (AWS EKS)
- **CI/CD**: GitHub Actions
- **Registry**: AWS ECR

## 📋 사전 요구사항

1. **AWS 계정 및 CLI 설정**
   ```bash
   aws configure
   ```

2. **kubectl 설치**
   ```bash
   # Windows (Chocolatey)
   choco install kubernetes-cli
   
   # macOS (Homebrew)
   brew install kubectl
   
   # Linux
   curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
   ```

3. **eksctl 설치**
   ```bash
   # Windows (Chocolatey)
   choco install eksctl
   
   # macOS (Homebrew)
   brew install eksctl
   
   # Linux
   curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
   sudo mv /tmp/eksctl /usr/local/bin
   ```

4. **Helm 설치**
   ```bash
   # Windows (Chocolatey)
   choco install kubernetes-helm
   
   # macOS (Homebrew)
   brew install helm
   
   # Linux
   curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
   ```

## 🚀 배포 단계

### 1단계: AWS 인프라 설정

```bash
# AWS ECR 및 EKS 설정
chmod +x aws-setup.sh
./aws-setup.sh
```

### 2단계: GitHub Secrets 설정

GitHub 저장소의 Settings > Secrets and variables > Actions에서 다음 시크릿을 추가하세요:

- `AWS_ACCESS_KEY_ID`: AWS 액세스 키 ID
- `AWS_SECRET_ACCESS_KEY`: AWS 시크릿 액세스 키

### 3단계: 환경 변수 설정

`k8s/configmap.yaml`에서 다음 값들을 실제 값으로 수정하세요:

```yaml
data:
  DB_URL: "jdbc:mysql://your-actual-rds-endpoint:3306/togather"
  DB_USERNAME: "your-actual-username"
  JWT_SECRET_KEY: "your-actual-jwt-secret-key"
```

`k8s/secret.yaml`에서 다음 값들을 Base64로 인코딩하여 설정하세요:

```bash
echo -n "your-actual-password" | base64
```

### 4단계: ECR 레지스트리 정보 업데이트

`.github/workflows/deploy.yml`에서 다음 값을 실제 값으로 수정하세요:

```yaml
env:
  ECR_REGISTRY: your-actual-account-id.dkr.ecr.ap-northeast-2.amazonaws.com
```

### 5단계: 배포 실행

```bash
# main 브랜치에 푸시하면 자동으로 배포가 시작됩니다
git add .
git commit -m "Initial deployment setup"
git push origin main
```

## 🔍 배포 확인

### 1. Pod 상태 확인
```bash
kubectl get pods -n togather
```

### 2. 서비스 상태 확인
```bash
kubectl get services -n togather
```

### 3. Ingress 상태 확인
```bash
kubectl get ingress -n togather
```

### 4. 로그 확인
```bash
kubectl logs -f deployment/api-gateway -n togather
kubectl logs -f deployment/user-service -n togather
```

## 🌐 서비스 접근

배포 완료 후 다음 URL로 서비스에 접근할 수 있습니다:

- **API Gateway**: `http://your-load-balancer-url/api/`
- **Health Check**: `http://your-load-balancer-url/api/health`
- **RabbitMQ Management**: `http://your-load-balancer-url:15672`

## 🔧 JWT 토큰 사용법

### 1. 로그인 (예시)
```bash
curl -X POST http://your-load-balancer-url/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "password"}'
```

### 2. API 호출 (JWT 토큰 포함)
```bash
curl -X GET http://your-load-balancer-url/api/users \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 📊 모니터링

### 1. HPA 상태 확인
```bash
kubectl get hpa -n togather
```

### 2. 메트릭 확인
```bash
kubectl top pods -n togather
kubectl top nodes
```

## 🛠️ 트러블슈팅

### 1. Pod가 시작되지 않는 경우
```bash
kubectl describe pod <pod-name> -n togather
kubectl logs <pod-name> -n togather
```

### 2. 서비스 연결 문제
```bash
kubectl get endpoints -n togather
kubectl describe service <service-name> -n togather
```

### 3. Ingress 문제
```bash
kubectl describe ingress togather-ingress -n togather
```

## 🔄 무중단 배포

이 설정은 무중단 배포를 지원합니다:

1. **Rolling Update**: Kubernetes의 기본 배포 전략
2. **Health Checks**: Liveness 및 Readiness 프로브
3. **HPA**: 자동 스케일링
4. **Load Balancer**: 트래픽 분산

## 📝 주요 특징

- ✅ **JWT 인증**: API Gateway에서 JWT 토큰 검증 및 사용자 정보 전달
- ✅ **마이크로서비스 통신**: RabbitMQ를 통한 이벤트 기반 통신
- ✅ **리버스 프록시**: Nginx를 통한 API 라우팅
- ✅ **자동 스케일링**: HPA를 통한 수평 확장
- ✅ **무중단 배포**: Rolling Update 전략
- ✅ **모니터링**: Actuator를 통한 헬스 체크
- ✅ **CI/CD**: GitHub Actions를 통한 자동 배포

## 🆘 지원

문제가 발생하면 다음을 확인하세요:

1. GitHub Actions 로그
2. Kubernetes 이벤트: `kubectl get events -n togather`
3. 서비스 로그: `kubectl logs -f deployment/<service-name> -n togather`
