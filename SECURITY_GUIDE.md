# 🔒 ToGather 보안 가이드

## 🛡️ 보안 강화 사항

### 1. 민감한 데이터 보호

#### ✅ GitHub Secrets 활용
- **DB_PASSWORD**: 데이터베이스 비밀번호
- **DB_URL**: 데이터베이스 연결 URL
- **DB_USERNAME**: 데이터베이스 사용자명
- **JWT_SECRET_KEY**: JWT 토큰 서명 키
- **GH_PAT**: GitHub Personal Access Token

#### ✅ Kubernetes Secret 분리
```yaml
# 민감한 데이터는 Secret으로 관리
apiVersion: v1
kind: Secret
metadata:
  name: togather-secret
type: Opaque
data:
  DB_PASSWORD: <base64-encoded>
  JWT_SECRET_KEY: <base64-encoded>
  RABBITMQ_PASSWORD: <base64-encoded>
```

#### ✅ ConfigMap 분리
```yaml
# 비민감한 설정만 ConfigMap으로 관리
apiVersion: v1
kind: ConfigMap
metadata:
  name: togather-config
data:
  RABBITMQ_HOST: "rabbitmq-service"
  RABBITMQ_PORT: "5672"
  API_GATEWAY_PORT: "8000"
```

### 2. 환경별 설정 분리

#### 개발 환경
```yaml
SPRING_PROFILES_ACTIVE: "dev"
LOGGING_LEVEL_ROOT: "INFO"
LOGGING_LEVEL_COM_EXAMPLE: "DEBUG"
```

#### 프로덕션 환경
```yaml
SPRING_PROFILES_ACTIVE: "prod"
LOGGING_LEVEL_ROOT: "WARN"
LOGGING_LEVEL_COM_EXAMPLE: "INFO"
```

### 3. RBAC (Role-Based Access Control)

```yaml
# 최소 권한 원칙 적용
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: togather-role
rules:
- apiGroups: [""]
  resources: ["configmaps", "secrets"]
  verbs: ["get", "list"]  # 읽기 전용 권한만 부여
```

### 4. 네트워크 보안

#### Service Mesh (선택사항)
```yaml
# Istio를 사용한 서비스 간 통신 암호화
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
spec:
  mtls:
    mode: STRICT
```

#### Network Policies
```yaml
# 네트워크 정책으로 트래픽 제한
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: togather-network-policy
spec:
  podSelector:
    matchLabels:
      app: togather
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: nginx
```

## 🔐 보안 체크리스트

### 배포 전 확인사항

- [ ] GitHub Secrets에 모든 민감한 데이터가 설정되어 있는가?
- [ ] k8s YAML 파일에 하드코딩된 비밀번호가 없는가?
- [ ] JWT_SECRET_KEY가 충분히 복잡한가? (최소 32자)
- [ ] 데이터베이스 연결이 SSL/TLS로 암호화되어 있는가?
- [ ] RabbitMQ 연결이 암호화되어 있는가?

### 런타임 보안

- [ ] 모든 서비스가 HTTPS를 사용하는가?
- [ ] API Gateway에서 JWT 토큰 검증이 제대로 작동하는가?
- [ ] 로그에 민감한 정보가 노출되지 않는가?
- [ ] 헬스 체크 엔드포인트가 적절히 보호되어 있는가?

## 🚨 보안 모니터링

### 1. 로그 모니터링
```bash
# 의심스러운 활동 모니터링
kubectl logs -f deployment/api-gateway -n togather | grep -i "error\|unauthorized\|forbidden"
```

### 2. 리소스 모니터링
```bash
# 비정상적인 리소스 사용량 확인
kubectl top pods -n togather
kubectl get events -n togather --sort-by='.lastTimestamp'
```

### 3. 네트워크 모니터링
```bash
# 네트워크 정책 확인
kubectl get networkpolicies -n togather
kubectl describe networkpolicy togather-network-policy -n togather
```

## 🔄 보안 업데이트

### 1. 정기적인 비밀번호 변경
```bash
# Secret 업데이트
kubectl create secret generic togather-secret \
  --from-literal=DB_PASSWORD="new-password" \
  --from-literal=JWT_SECRET_KEY="new-jwt-secret" \
  --namespace=togather \
  --dry-run=client -o yaml | kubectl apply -f -
```

### 2. 이미지 보안 스캔
```bash
# Docker 이미지 취약점 스캔
trivy image your-ecr-account.dkr.ecr.region.amazonaws.com/togather/api-gateway:latest
```

### 3. Kubernetes 보안 스캔
```bash
# kube-score로 YAML 보안 검사
kube-score score k8s/*.yaml
```

## 🆘 보안 사고 대응

### 1. 비상 계획
1. **즉시 조치**: 의심스러운 Pod 즉시 종료
2. **격리**: 네트워크 정책으로 트래픽 차단
3. **조사**: 로그 분석 및 영향 범위 파악
4. **복구**: 보안 패치 적용 후 재배포

### 2. 연락처
- **보안 담당자**: security@togather.com
- **긴급 연락처**: +82-10-1234-5678
- **Slack 채널**: #security-alerts

## 📋 보안 정책

### 1. 비밀번호 정책
- 최소 12자 이상
- 대소문자, 숫자, 특수문자 포함
- 90일마다 변경

### 2. 접근 제어
- 최소 권한 원칙 적용
- 2FA 인증 필수
- 정기적인 권한 검토

### 3. 데이터 보호
- 전송 중 암호화 (TLS 1.3)
- 저장 시 암호화 (AES-256)
- 정기적인 백업 및 복구 테스트

## 🔍 보안 도구

### 1. 정적 분석
- **kube-score**: Kubernetes YAML 보안 검사
- **kubeaudit**: Kubernetes 보안 감사
- **trivy**: 컨테이너 이미지 취약점 스캔

### 2. 동적 분석
- **Falco**: 런타임 보안 모니터링
- **OPA Gatekeeper**: 정책 기반 제어
- **Istio**: 서비스 메시 보안

### 3. 모니터링
- **Prometheus + Grafana**: 메트릭 수집 및 시각화
- **ELK Stack**: 로그 수집 및 분석
- **Jaeger**: 분산 추적

---

**⚠️ 중요**: 이 가이드는 기본적인 보안 조치를 제공합니다. 프로덕션 환경에서는 추가적인 보안 검토와 전문가 상담이 필요합니다.
