#!/usr/bin/env bash
# 측정 세션 시작 "딸깍" 스크립트.
#   (이미지 없으면 빌드/푸시) → secrets.auto.tfvars 갱신 → test apply
#   → 앱 healthy 대기(Flyway 완료 보장) → 시드 적재 → 접속 안내
#
# 사용법:
#   script/aws/up.sh                     # 기본: --seed view-count (post-list 측정까지 커버)
#   script/aws/up.sh --seed post-list    # 01~05a
#   script/aws/up.sh --seed view-count   # 01~05a + 05c
#   script/aws/up.sh --seed full         # + 06~08 (댓글 300만 — 오래 걸림)
#   script/aws/up.sh --seed none         # 시드 생략
#   script/aws/up.sh --8m                # 핫보드 +700만(05b)도 적재
#   script/aws/up.sh --push              # 이미지 강제 재빌드/푸시
source "$(dirname "$0")/lib.sh"

SEED_PROFILE="view-count"; PUSH=0; EIGHT_M=0
while [ $# -gt 0 ]; do
  case "$1" in
    --seed) SEED_PROFILE="${2:?--seed 뒤에 프로파일}"; shift 2 ;;
    --8m)   EIGHT_M=1; shift ;;
    --push) PUSH=1; shift ;;
    *) die "알 수 없는 옵션: $1 (사용법은 파일 상단 주석)" ;;
  esac
done
case "$SEED_PROFILE" in post-list|view-count|full|none) ;; *) die "--seed 는 post-list|view-count|full|none" ;; esac

require_aws
require_base
[ -f "$SSH_PUB" ] || die "$SSH_PUB 이 없습니다 (bastion 시딩/터널에 필요)"

# ── 1) ECR 이미지 확보 ──────────────────────────────────────────
ECR_URL="$(base_out ecr_repository_url)"
REPO_NAME="${ECR_URL##*/}"
TAG="${IMAGE_TAG:-latest}"
if [ "$PUSH" = 1 ] || ! aws ecr describe-images --repository-name "$REPO_NAME" \
     --image-ids imageTag="$TAG" --region "$AWS_REGION" >/dev/null 2>&1; then
  "$SCRIPT_DIR/push-image.sh" "$TAG"
else
  log "ECR에 :$TAG 이미지가 이미 있어 빌드를 건너뜁니다 (강제 재빌드는 --push)"
fi

# ── 2) secrets.auto.tfvars 갱신 ────────────────────────────────
# db_password는 최초 1회 생성 후 유지, my_ip는 매번 현재 IP로 갱신.
# apply/destroy 둘 다 이 파일을 자동으로 읽으므로 -var 입력이 필요 없다.
DB_PASSWORD=""
[ -f "$SECRETS_FILE" ] && DB_PASSWORD="$(sed -n 's/^db_password[[:space:]]*=[[:space:]]*"\(.*\)"/\1/p' "$SECRETS_FILE")"
if [ -z "$DB_PASSWORD" ]; then
  # openssl 사용: pipefail 아래에서 tr|head 파이프라인은 SIGPIPE로 스크립트를 죽인다
  DB_PASSWORD="$(openssl rand -hex 16)"
  log "db_password 신규 생성 → $SECRETS_FILE (gitignore 대상)"
fi
MY_IP="$(my_ip_cidr)"
cat > "$SECRETS_FILE" <<EOF
# script/aws/up.sh 가 생성/갱신하는 파일 — 직접 수정할 필요 없음 (*.tfvars는 gitignore)
my_ip          = "$MY_IP"
db_password    = "$DB_PASSWORD"
ssh_public_key = "$(cat "$SSH_PUB")"
EOF
log "my_ip=$MY_IP 반영"

# ── 3) terraform apply (test) ──────────────────────────────────
tf_init_if_needed "$TEST_DIR"
log "terraform apply (test) — NAT/ECS/MySQL/Redis/모니터링/bastion 생성"
tf_test apply -auto-approve

# ── 4) 앱 healthy 대기 ─────────────────────────────────────────
# 타깃그룹 healthy = MySQL 준비 + Spring 부팅 + Flyway 마이그레이션 완료 → 시드 가능 시점
TG_ARN="$(base_out target_group_arn)"
log "ALB 타깃 healthy 대기 (user_data 설치 + Spring 부팅, 보통 5~10분)"
DEADLINE=$((SECONDS + 1500))
while :; do
  STATE="$(aws elbv2 describe-target-health --target-group-arn "$TG_ARN" --region "$AWS_REGION" \
    --query 'TargetHealthDescriptions[].TargetHealth.State' --output text 2>/dev/null | tr '\t' ',')"
  [ -z "$STATE" ] && STATE="타깃 미등록"
  case ",$STATE," in *,healthy,*) break ;; esac
  if [ $SECONDS -ge $DEADLINE ]; then
    echo
    warn "25분 내 healthy가 되지 않았습니다. 최근 ECS 서비스 이벤트:"
    aws ecs describe-services --cluster "$ECS_CLUSTER" --services "$ECS_SERVICE" \
      --region "$AWS_REGION" --query 'services[0].events[:10].message' --output text 2>/dev/null || true
    die "헬스체크 타임아웃 — 위 이벤트와 CloudWatch 로그를 확인하세요"
  fi
  printf '  %s 타깃 상태: %-24s (%ds 경과)\r' "$(date +%H:%M:%S)" "$STATE" "$SECONDS"
  sleep 15
done
echo
log "앱 healthy — https://$DOMAIN 응답 가능"

# ── 5) 시드 적재 ───────────────────────────────────────────────
if [ "$SEED_PROFILE" != "none" ]; then
  SEED_ARGS=("$SEED_PROFILE" --wait)
  [ "$EIGHT_M" = 1 ] && SEED_ARGS+=(--8m)
  "$SCRIPT_DIR/seed.sh" "${SEED_ARGS[@]}"
fi

# ── 6) 요약 ────────────────────────────────────────────────────
echo
log "━━━ 준비 완료 ━━━"
cat <<SUMMARY
  API          : https://$DOMAIN  (헬스: curl https://$DOMAIN/actuator/health)
  모니터링 터널: script/aws/tunnel.sh start   → Grafana http://localhost:3000 (admin/admin)
  k6 예시      : k6 run -e BASE_URL=https://$DOMAIN -e VERSION=v3 -e RATE=5 -e DURATION=10s script/view-count/k6/view-count-bench.k6.js
  측정 종료 시 : script/aws/down.sh   (NAT/EC2 전부 제거 — 시간당 과금 정지)
SUMMARY
