# 🔐 Module-Common 보안 설정 가이드

## ⚠️ 중요: 보안 설정

`module-common`의 설정 파일들은 민감한 정보를 포함하고 있어 Git에 커밋되지 않습니다.

## 📁 파일 구조

```
module-common/src/main/resources/
├── application.yml              # ❌ Git에 커밋되지 않음 (민감한 정보 포함)
├── application-dev.yml          # ✅ Git에 커밋됨 (개발용 템플릿)
├── application-main.yml         # ✅ Git에 커밋됨 (운영용 템플릿)
└── application.yml.template     # ✅ Git에 커밋됨 (참고용)
```

## 🚀 설정 방법

### 1. 로컬 개발 환경 설정

```bash
# 1. application.yml 파일 생성
cp module-common/src/main/resources/application-dev.yml module-common/src/main/resources/application.yml

# 2. 실제 값으로 수정
# - 데이터베이스 비밀번호
# - Redis 비밀번호  
# - RabbitMQ 비밀번호
# - JWT 시크릿 키
# - GitHub PAT
```

### 2. 운영 환경 설정

운영 환경에서는 환경변수로 모든 값을 주입받습니다:

```yaml
# application-main.yml이 사용됨
# 모든 값은 환경변수에서 주입
```

## 🔧 환경변수 목록

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `DB_URL` | 데이터베이스 URL | - |
| `DB_USERNAME` | 데이터베이스 사용자명 | - |
| `DB_PASSWORD` | 데이터베이스 비밀번호 | - |
| `REDIS_HOST` | Redis 호스트 | localhost |
| `REDIS_PORT` | Redis 포트 | 6379 |
| `REDIS_PASSWORD` | Redis 비밀번호 | - |
| `RABBITMQ_HOST` | RabbitMQ 호스트 | localhost |
| `RABBITMQ_PORT` | RabbitMQ 포트 | 5672 |
| `RABBITMQ_USERNAME` | RabbitMQ 사용자명 | guest |
| `RABBITMQ_PASSWORD` | RabbitMQ 비밀번호 | - |
| `JWT_SECRET_KEY` | JWT 시크릿 키 | - |
| `JWT_ACCESS_EXP` | JWT 액세스 토큰 만료시간(초) | 1800 |
| `JWT_REFRESH_EXP` | JWT 리프레시 토큰 만료시간(일) | 7 |
| `GH_PAT` | GitHub Personal Access Token | - |

## ⚠️ 주의사항

1. **절대 실제 비밀번호를 Git에 커밋하지 마세요**
2. **application.yml 파일은 .gitignore에 포함되어 있습니다**
3. **팀원들과 실제 비밀번호는 안전한 방법으로 공유하세요**
4. **운영 환경에서는 반드시 환경변수를 사용하세요**

## 🔄 업데이트 방법

새로운 설정이 추가되면:

1. `application-dev.yml`과 `application-main.yml` 업데이트
2. 로컬 `application.yml` 파일 수동 업데이트
3. 팀원들에게 변경사항 공유
