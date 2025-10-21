#!/bin/bash

# S3 버킷 생성 및 CloudFront 설정 스크립트

BUCKET_NAME="togather-static-assets"
REGION="ap-northeast-2"
CLOUDFRONT_DISTRIBUTION_ID="E15ZDIW40YBVEN"

echo "🚀 S3 버킷 생성 중: $BUCKET_NAME"

# S3 버킷 생성
aws s3 mb s3://$BUCKET_NAME --region $REGION

# 버킷 정책 설정 (CloudFront에서 접근 가능하도록)
cat > bucket-policy.json << EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowCloudFrontServicePrincipal",
            "Effect": "Allow",
            "Principal": {
                "Service": "cloudfront.amazonaws.com"
            },
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::$BUCKET_NAME/*",
            "Condition": {
                "StringEquals": {
                    "AWS:SourceArn": "arn:aws:cloudfront::340623336075:distribution/$CLOUDFRONT_DISTRIBUTION_ID"
                }
            }
        }
    ]
}
EOF

# 버킷 정책 적용
aws s3api put-bucket-policy --bucket $BUCKET_NAME --policy file://bucket-policy.json

# CORS 설정
cat > cors-config.json << EOF
{
    "CORSRules": [
        {
            "AllowedHeaders": ["*"],
            "AllowedMethods": ["GET", "HEAD"],
            "AllowedOrigins": ["*"],
            "ExposeHeaders": ["ETag"],
            "MaxAgeSeconds": 3000
        }
    ]
}
EOF

aws s3api put-bucket-cors --bucket $BUCKET_NAME --cors-configuration file://cors-config.json

# 정적 웹사이트 호스팅 비활성화 (CloudFront 사용)
aws s3api put-bucket-website --bucket $BUCKET_NAME --website-configuration '{"IndexDocument":{"Suffix":"index.html"},"ErrorDocument":{"Key":"error.html"}}'

echo "✅ S3 버킷 설정 완료!"
echo "📝 버킷 이름: $BUCKET_NAME"
echo "🌐 CloudFront Distribution ID: $CLOUDFRONT_DISTRIBUTION_ID"

# 정리
rm bucket-policy.json cors-config.json

echo "🎉 S3 + CloudFront 설정 완료!"
