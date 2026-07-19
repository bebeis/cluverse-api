#!/usr/bin/env bash
# bootJar 빌드 → linux/amd64 이미지 빌드 → ECR 푸시.
# test 스택이 이미 떠 있으면 ECS 롤링 재배포까지 수행한다.
#
# 사용법: script/aws/push-image.sh [태그]   (기본 latest)
source "$(dirname "$0")/lib.sh"

TAG="${1:-latest}"

require_aws
require_base
docker info >/dev/null 2>&1 || die "Docker 데몬이 실행 중이 아닙니다"

ECR_URL="$(base_out ecr_repository_url)"
REGISTRY="${ECR_URL%%/*}"

log "bootJar 빌드 (테스트/asciidoctor 생략)"
(cd "$REPO_ROOT" && ./gradlew -q bootJar -x test -x asciidoctor)
# Dockerfile의 COPY build/libs/*.jar 가 단일 파일을 전제하므로 과거 빌드의 plain jar를 제거
rm -f "$REPO_ROOT"/build/libs/*-plain.jar

log "ECR 로그인 및 이미지 빌드/푸시: $ECR_URL:$TAG (linux/amd64)"
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$REGISTRY"
docker build --platform linux/amd64 -t "$ECR_URL:$TAG" "$REPO_ROOT"
docker push "$ECR_URL:$TAG"

if [ "$(aws ecs describe-services --cluster "$ECS_CLUSTER" --services "$ECS_SERVICE" \
        --region "$AWS_REGION" --query 'services[0].status' --output text 2>/dev/null)" = "ACTIVE" ]; then
  log "test 스택이 떠 있어 ECS 서비스를 새 이미지로 재배포합니다"
  aws ecs update-service --cluster "$ECS_CLUSTER" --service "$ECS_SERVICE" \
    --force-new-deployment --region "$AWS_REGION" >/dev/null
fi
log "완료"
