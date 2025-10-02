FROM nginx:1.25-alpine

# Nginx 기본 설정 삭제
RUN rm /etc/nginx/conf.d/default.conf

# 커스텀 Nginx 설정 파일 복사
COPY ./nginx/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]