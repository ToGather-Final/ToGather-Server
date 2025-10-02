FROM nginx:1.25-alpine

# Nginx 컨테이너 내부의 기본 설정 파일을 삭제합니다.
# 이 파일을 삭제해야 아래에서 복사하는 우리만의 설정이 적용됩니다.
RUN rm /etc/nginx/conf.d/default.conf

# 우리가 직접 작성한 Nginx 설정 파일을 컨테ainer 내부의 설정 폴더로 복사합니다.
# COPY <내 PC의 파일 경로> <컨테이너 내부 경로>
COPY ./nginx/nginx.conf /etc/nginx/conf.d/default.conf

# GitHub Actions가 체크아웃하고 빌드한 'Togather-Client'의 결과물을
# Nginx의 기본 웹사이트 폴더로 복사합니다.
COPY ./togather-client/build /usr/share/nginx/html

# 컨테이너가 80번 포트를 사용함을 명시합니다.
EXPOSE 80

# 컨테이너가 시작될 때 Nginx를 실행하는 명령어입니다.
CMD ["nginx", "-g", "daemon off;"]