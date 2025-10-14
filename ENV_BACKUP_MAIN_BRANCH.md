# ToGather 환경 변수 백업 (Main Branch)

**백업 날짜**: 2025-10-13  
**브랜치**: main (AWS EKS 배포 전용)  
**네임스페이스**: togather

---

## 📋 목차
1. [Kubernetes Secret 생성 명령어](#1-kubernetes-secret-생성-명령어)
2. [ConfigMap 환경 변수](#2-configmap-환경-변수)
3. [서비스별 환경 변수 상세](#3-서비스별-환경-변수-상세)
4. [완전 복원 가이드](#4-완전-복원-가이드)

---

## 1. Kubernetes Secret 생성 명령어

### 1.1 togather-secrets (민감 정보)
```bash
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
```

---

## 2. ConfigMap 환경 변수

### 2.1 togather-config (공통 설정)
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: togather-config
  namespace: togather
data:
  SPRING_RABBITMQ_HOST: "rabbitmq"
  SPRING_DATA_REDIS_HOST: "redis"
  SPRING_DATA_REDIS_PORT: "6379"
```

### 2.2 togather-db-config (데이터베이스 설정)
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: togather-db-config
  namespace: togather
data:
  SPRING_DATASOURCE_URL: "jdbc:mysql://togather-database.cxs46swiy72b.ap-northeast-2.rds.amazonaws.com:3306/togather_db"
```

### 2.3 togather-env-config (환경 설정)
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: togather-env-config
  namespace: togather
data:
  LOGGING_LEVEL_ROOT: "INFO"
  LOGGING_LEVEL_COM_EXAMPLE: "DEBUG"
```

---

## 3. 서비스별 환경 변수 상세

### 3.1 API Gateway (포트: 8000)

#### Deployment 환경 변수
```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "main"
  - name: SPRING_DATASOURCE_URL
    value: ""  # 데이터소스 비활성화
  - name: SPRING_DATASOURCE_USERNAME
    value: ""
  - name: SPRING_DATASOURCE_PASSWORD
    value: ""
  - name: SPRING_JPA_HIBERNATE_DDL_AUTO
    value: "none"
  - name: SPRING_JPA_SHOW_SQL
    value: "false"
  - name: LOGGING_LEVEL_COM_EXAMPLE_API_GATEWAY
    value: "DEBUG"
  - name: LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CLOUD_GATEWAY
    value: "INFO"
  - name: LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB_REACTIVE
    value: "INFO"
  - name: LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY
    value: "INFO"
  - name: LOGGING_LEVEL_REACTOR_NETTY
    value: "INFO"
  - name: SPRING_DATA_REDIS_HOST
    value: "redis"
  - name: SPRING_DATA_REDIS_PORT
    value: "6379"
  - name: SPRING_DATA_REDIS_PASSWORD
    valueFrom:
      secretKeyRef:
        name: togather-secrets
        key: REDIS_PASSWORD

envFrom:
  - configMapRef:
      name: togather-config
  - secretRef:
      name: togather-secrets
```

#### application.yml (main 프로파일)
```yaml
spring:
  profiles:
    active: main
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:redis}
      port: ${SPRING_DATA_REDIS_PORT:6379}
      password: ${SPRING_DATA_REDIS_PASSWORD:}
      timeout: 5000ms
      connect-timeout: 5000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: 5000ms
        shutdown-timeout: 100ms

jwt:
  secret: ${JWT_SECRET_KEY:TogatherSecretkey}

server:
  port: 8000
```

---

### 3.2 User Service (포트: 8080)

#### Deployment 환경 변수
```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "main"

envFrom:
  - configMapRef:
      name: togather-config
  - configMapRef:
      name: togather-db-config
  - secretRef:
      name: togather-secrets
```

#### application.yml (main 프로파일)
```yaml
spring:
  profiles:
    active: main
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:redis}
      port: ${SPRING_DATA_REDIS_PORT:6379}
      password: ${SPRING_DATA_REDIS_PASSWORD:}
  rabbitmq:
    host: ${RABBITMQ_HOST:rabbitmq}
    port: 5672
    username: ${RABBITMQ_USERNAME:admin}
    password: ${RABBITMQ_PASSWORD:}

jwt:
  secret-key: ${JWT_SECRET_KEY}
  access-exp-seconds: 1800
  refresh-exp-days: 7

server:
  port: 8080
```

#### 사용하는 환경 변수 목록
- `SPRING_PROFILES_ACTIVE=main`
- `DB_URL` (from togather-db-config: SPRING_DATASOURCE_URL)
- `DB_USERNAME` (from togather-secrets: admin)
- `DB_PASSWORD` (from togather-secrets: togather1234)
- `SPRING_DATA_REDIS_HOST=redis` (from togather-config)
- `SPRING_DATA_REDIS_PORT=6379` (from togather-config)
- `SPRING_DATA_REDIS_PASSWORD=togather1234` (from togather-secrets)
- `RABBITMQ_HOST=rabbitmq` (from togather-config)
- `RABBITMQ_USERNAME=admin` (from togather-secrets)
- `RABBITMQ_PASSWORD=togather1234` (from togather-secrets)
- `JWT_SECRET_KEY=TogatherSecretkey` (from togather-secrets)

---

### 3.3 Vote Service (포트: 8080)

#### Deployment 환경 변수
```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "main"
  - name: SPRING_DATA_REDIS_HOST
    value: "redis"
  - name: SPRING_DATA_REDIS_PORT
    value: "6379"
  - name: SPRING_DATA_REDIS_PASSWORD
    valueFrom:
      secretKeyRef:
        name: togather-secrets
        key: REDIS_PASSWORD

envFrom:
  - configMapRef:
      name: togather-config
  - configMapRef:
      name: togather-db-config
  - secretRef:
      name: togather-secrets
```

#### application.yml (main 프로파일)
```yaml
spring:
  profiles:
    active: main
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  data:
    redis:
      host: redis
      port: 6379
      password: ${SPRING_DATA_REDIS_PASSWORD:}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
  rabbitmq:
    host: rabbitmq
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}

jwt:
  secret: ${JWT_SECRET_KEY}

server:
  port: 8080
```

#### 사용하는 환경 변수 목록
- `SPRING_PROFILES_ACTIVE=main`
- `SPRING_DATASOURCE_URL` (from togather-db-config)
- `DB_USERNAME=admin` (from togather-secrets)
- `DB_PASSWORD=togather1234` (from togather-secrets)
- `SPRING_DATA_REDIS_HOST=redis`
- `SPRING_DATA_REDIS_PORT=6379`
- `SPRING_DATA_REDIS_PASSWORD=togather1234` (from togather-secrets)
- `RABBITMQ_HOST=rabbitmq` (from togather-config)
- `RABBITMQ_USERNAME=admin` (from togather-secrets)
- `RABBITMQ_PASSWORD=togather1234` (from togather-secrets)
- `JWT_SECRET_KEY=TogatherSecretkey` (from togather-secrets)

---

### 3.4 Trading Service (포트: 8080)

#### Deployment 환경 변수
```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "main"

envFrom:
  - configMapRef:
      name: togather-config
  - configMapRef:
      name: togather-db-config
  - secretRef:
      name: togather-secrets
```

#### application.yml (main 프로파일)
```yaml
spring:
  profiles:
    active: main
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:redis}
      port: ${SPRING_DATA_REDIS_PORT:6379}
      password: ${SPRING_DATA_REDIS_PASSWORD:}
  rabbitmq:
    host: ${RABBITMQ_HOST:rabbitmq}
    port: 5672
    username: ${RABBITMQ_USERNAME:admin}
    password: ${RABBITMQ_PASSWORD:}

jwt:
  secret-key: ${JWT_SECRET_KEY}
  access-exp-seconds: 1800
  refresh-exp-days: 7

server:
  port: 8080
```

#### 사용하는 환경 변수 목록
- `SPRING_PROFILES_ACTIVE=main`
- `DB_URL` (from togather-db-config: SPRING_DATASOURCE_URL)
- `DB_USERNAME=admin` (from togather-secrets)
- `DB_PASSWORD=togather1234` (from togather-secrets)
- `SPRING_DATA_REDIS_HOST=redis` (from togather-config)
- `SPRING_DATA_REDIS_PORT=6379` (from togather-config)
- `SPRING_DATA_REDIS_PASSWORD=togather1234` (from togather-secrets)
- `RABBITMQ_HOST=rabbitmq` (from togather-config)
- `RABBITMQ_USERNAME=admin` (from togather-secrets)
- `RABBITMQ_PASSWORD=togather1234` (from togather-secrets)
- `JWT_SECRET_KEY=TogatherSecretkey` (from togather-secrets)

---

### 3.5 Pay Service (포트: 8080)

#### Deployment 환경 변수
```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "main"

envFrom:
  - configMapRef:
      name: togather-config
  - configMapRef:
      name: togather-db-config
  - secretRef:
      name: togather-secrets
```

#### application.yml (main 프로파일)
```yaml
spring:
  profiles:
    active: main
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
  rabbitmq:
    host: ${SPRING_RABBITMQ_HOST:rabbitmq}
    port: 5672
    username: ${SPRING_RABBITMQ_USERNAME:admin}
    password: ${SPRING_RABBITMQ_PASSWORD}
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: 6379
      password: ${SPRING_DATA_REDIS_PASSWORD:}

jwt:
  secret-key: ${JWT_SECRET_KEY:togather-jwt-secret-key-2024-default}
  expiration-in-ms: 86400000

pay:
  demo-mode: false

server:
  port: 8080
```

#### 사용하는 환경 변수 목록
- `SPRING_PROFILES_ACTIVE=main`
- `SPRING_DATASOURCE_URL` (from togather-db-config)
- `DB_USERNAME=admin` (from togather-secrets)
- `DB_PASSWORD=togather1234` (from togather-secrets)
- `SPRING_RABBITMQ_HOST=rabbitmq` (from togather-config)
- `SPRING_RABBITMQ_USERNAME=admin` (from togather-secrets)
- `SPRING_RABBITMQ_PASSWORD=togather1234` (from togather-secrets)
- `SPRING_DATA_REDIS_HOST=redis` (from togather-config)
- `SPRING_DATA_REDIS_PORT=6379` (from togather-config)
- `SPRING_DATA_REDIS_PASSWORD=togather1234` (from togather-secrets)
- `JWT_SECRET_KEY=TogatherSecretkey` (from togather-secrets)

---

## 4. 완전 복원 가이드

### 4.1 Secret 생성
```bash
# 1. togather-secrets 생성
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
```

### 4.2 ConfigMap 생성
```bash
# 2. togather-config 생성
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: togather-config
  namespace: togather
data:
  SPRING_RABBITMQ_HOST: "rabbitmq"
  SPRING_DATA_REDIS_HOST: "redis"
  SPRING_DATA_REDIS_PORT: "6379"
EOF

# 3. togather-db-config 생성
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: togather-db-config
  namespace: togather
data:
  SPRING_DATASOURCE_URL: "jdbc:mysql://togather-database.cxs46swiy72b.ap-northeast-2.rds.amazonaws.com:3306/togather_db"
EOF

# 4. togather-env-config 생성
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: togather-env-config
  namespace: togather
data:
  LOGGING_LEVEL_ROOT: "INFO"
  LOGGING_LEVEL_COM_EXAMPLE: "DEBUG"
EOF
```

### 4.3 전체 환경 변수 매핑 표

| 환경 변수 | 값 | 소스 | 사용 서비스 |
|-----------|-----|------|------------|
| `SPRING_PROFILES_ACTIVE` | `main` | Deployment | 전체 |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://togather-database.cxs46swiy72b.ap-northeast-2.rds.amazonaws.com:3306/togather_db` | togather-db-config | user, vote, trading, pay |
| `SPRING_DATASOURCE_USERNAME` | `admin` | togather-secrets | user, vote, trading, pay |
| `SPRING_DATASOURCE_PASSWORD` | `togather1234` | togather-secrets | user, vote, trading, pay |
| `DB_USERNAME` | `admin` | togather-secrets | user, vote, trading, pay |
| `DB_PASSWORD` | `togather1234` | togather-secrets | user, vote, trading, pay |
| `JWT_SECRET_KEY` | `TogatherSecretkey` | togather-secrets | 전체 |
| `JWT_SECRET` | `TogatherSecretkey` | togather-secrets | api-gateway |
| `SPRING_RABBITMQ_HOST` | `rabbitmq` | togather-config | user, vote, trading, pay |
| `SPRING_RABBITMQ_USERNAME` | `admin` | togather-secrets | user, vote, trading, pay |
| `SPRING_RABBITMQ_PASSWORD` | `togather1234` | togather-secrets | user, vote, trading, pay |
| `RABBITMQ_HOST` | `rabbitmq` | togather-config | user, trading |
| `RABBITMQ_USERNAME` | `admin` | togather-secrets | user, vote, trading |
| `RABBITMQ_PASSWORD` | `togather1234` | togather-secrets | user, vote, trading |
| `SPRING_DATA_REDIS_HOST` | `redis` | togather-config | 전체 |
| `SPRING_DATA_REDIS_PORT` | `6379` | togather-config | 전체 |
| `SPRING_DATA_REDIS_PASSWORD` | `togather1234` | togather-secrets | 전체 |
| `REDIS_PASSWORD` | `togather1234` | togather-secrets | api-gateway, vote |

### 4.4 서비스 재시작 순서
1. RabbitMQ
2. Redis
3. User Service
4. Vote Service
5. Trading Service
6. Pay Service
7. API Gateway

```bash
# 순서대로 재시작
kubectl rollout restart deployment/rabbitmq -n togather
kubectl rollout restart deployment/redis -n togather
kubectl rollout restart deployment/user-service -n togather
kubectl rollout restart deployment/vote-service -n togather
kubectl rollout restart deployment/trading-service -n togather
kubectl rollout restart deployment/pay-service -n togather
kubectl rollout restart deployment/api-gateway -n togather
```

---

## 5. 주요 인프라 정보

### 5.1 데이터베이스
- **RDS 엔드포인트**: `togather-database.cxs46swiy72b.ap-northeast-2.rds.amazonaws.com:3306`
- **데이터베이스명**: `togather_db`
- **사용자**: `admin`
- **비밀번호**: `togather1234`

### 5.2 Redis
- **서비스명**: `redis`
- **포트**: `6379`
- **비밀번호**: `togather1234`

### 5.3 RabbitMQ
- **서비스명**: `rabbitmq`
- **포트**: `5672`
- **사용자**: `admin`
- **비밀번호**: `togather1234`

### 5.4 JWT
- **Secret Key**: `TogatherSecretkey`
- **Access Token 만료**: 1800초 (30분)
- **Refresh Token 만료**: 7일

---

## 6. 트러블슈팅

### 6.1 Secret이 없을 때
```bash
# Secret 확인
kubectl get secret togather-secrets -n togather

# Secret 재생성 (이전 것 삭제 후)
kubectl delete secret togather-secrets -n togather
# 위의 4.1 Secret 생성 명령어 실행
```

### 6.2 ConfigMap이 없을 때
```bash
# ConfigMap 확인
kubectl get configmap -n togather

# ConfigMap 재생성
kubectl delete configmap togather-config -n togather
kubectl delete configmap togather-db-config -n togather
kubectl delete configmap togather-env-config -n togather
# 위의 4.2 ConfigMap 생성 명령어 실행
```

### 6.3 Pod 재시작
```bash
# 특정 서비스 재시작
kubectl rollout restart deployment/<service-name> -n togather

# 모든 서비스 재시작
kubectl rollout restart deployment -n togather
```

---

## 7. 체크리스트

배포 전 확인사항:
- [ ] togather-secrets 생성 완료
- [ ] togather-config 생성 완료
- [ ] togather-db-config 생성 완료
- [ ] togather-env-config 생성 완료
- [ ] RDS 연결 가능 확인
- [ ] Redis Pod 정상 동작 확인
- [ ] RabbitMQ Pod 정상 동작 확인
- [ ] 모든 서비스 이미지 ECR에 푸시 완료
- [ ] 각 서비스의 application.yml에 main 프로파일 설정 확인
- [ ] API Gateway RBAC 설정 완료

---

**백업 완료일**: 2025-10-13  
**작성자**: ToGather Team  
**버전**: 1.0

