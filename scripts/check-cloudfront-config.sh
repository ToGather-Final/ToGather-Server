#!/bin/bash

# CloudFront Distribution 설정 확인 스크립트

DISTRIBUTION_ID="E15ZDIW40YBVEN"
S3_BUCKET_NAME="togather-static-assets"

echo "🔍 CloudFront Distribution 설정 확인 중..."

# 1. CloudFront Distribution 정보 확인
echo "📊 CloudFront Distribution 정보:"
aws cloudfront get-distribution --id $DISTRIBUTION_ID --query 'Distribution.DistributionConfig.Origins.Items[*].{DomainName:DomainName,Id:Id,OriginPath:OriginPath}' --output table

# 2. S3 버킷 정책 확인
echo "📊 S3 버킷 정책 확인:"
aws s3api get-bucket-policy --bucket $S3_BUCKET_NAME --query 'Policy' --output text | jq '.' 2>/dev/null || echo "❌ S3 버킷 정책이 설정되지 않았습니다."

# 3. S3 버킷 CORS 설정 확인
echo "📊 S3 버킷 CORS 설정 확인:"
aws s3api get-bucket-cors --bucket $S3_BUCKET_NAME --query 'CORSRules' --output table 2>/dev/null || echo "❌ S3 버킷 CORS가 설정되지 않았습니다."

# 4. CloudFront Cache Behaviors 확인
echo "📊 CloudFront Cache Behaviors 확인:"
aws cloudfront get-distribution --id $DISTRIBUTION_ID --query 'Distribution.DistributionConfig.CacheBehaviors.Items[*].{PathPattern:PathPattern,TargetOriginId:TargetOriginId,CachePolicyId:CachePolicyId}' --output table

echo "✅ CloudFront 설정 확인 완료!"
