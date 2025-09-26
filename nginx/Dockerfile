# 1. 기본 Nginx 이미지 사용
FROM nginx:latest

# 2. 기존 Nginx 설정 파일 복사
COPY ./nginx/nginx.conf /etc/nginx/nginx.conf

# 3. GitHub Actions에서 빌드한 클라이언트 정적 파일을 이미지 안으로 복사
#    워크플로우의 빌드 컨텍스트 루트에서 togather-client/build 경로를 찾음
COPY ./togather-client/build /usr/share/nginx/html