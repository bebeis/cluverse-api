#!/usr/bin/env bash
# 시드 SQL을 bastion에 올려 bastion에서 nohup으로 실행한다.
# 로컬 노트북이 잠들거나 네트워크가 끊겨도 시딩은 계속 진행된다 (100만 건 이상 = 수십 분).
# DB 비밀번호는 bastion이 SSM Parameter Store에서 직접 읽는다 (로컬로 노출 없음).
#
# 사용법:
#   script/aws/seed.sh view-count [--8m] [--30m] [--wait]   # 01~05a(+05b)(+05d) + 최종 조회수 Redis 초기화
#   script/aws/seed.sh post-list  [--8m] [--30m] [--wait]   # 01~05a(+05b)(+05d)
#   script/aws/seed.sh full       [--8m] [--30m] [--wait]   # view-count + 06_comment(300만)~08
#   script/aws/seed.sh view-count --dry-run                 # AWS 변경 없이 실행 계획 확인
#   script/aws/seed.sh --status                             # 마지막 로그 확인
#   script/aws/seed.sh --follow                             # 로그 실시간 tail
#
# --8m  : 핫보드 +700만(05b). --30m 과 독립 (id 범위가 겹치지 않는다)
# --30m : 일반 게시판 +1,600만(05d, post_id 상한 3,000만). 매우 오래 걸리고 디스크를
#         약 9GB 더 쓴다 — docs/v1/ddl/test-data/05d_post_seed_30m.sql 헤더 참고
source "$(dirname "$0")/lib.sh"

PROFILE=""; WAIT=0; EIGHT_M=0; THIRTY_M=0; DRY_RUN=0; ACTION="run"
while [ $# -gt 0 ]; do
  case "$1" in
    post-list|view-count|full) PROFILE="$1"; shift ;;
    --8m)     EIGHT_M=1; shift ;;
    --30m)    THIRTY_M=1; shift ;;
    --wait)   WAIT=1; shift ;;
    --dry-run) DRY_RUN=1; shift ;;
    --status) ACTION="status"; shift ;;
    --follow) ACTION="follow"; shift ;;
    *) die "알 수 없는 인자: $1 (사용법은 파일 상단 주석)" ;;
  esac
done

if [ "$ACTION" = "status" ]; then ssh_bastion 'tail -20 seed/seed.log 2>/dev/null || echo "시드 로그 없음"'; exit 0; fi
if [ "$ACTION" = "follow" ]; then ssh_bastion 'tail -f seed/seed.log'; exit 0; fi
[ -n "$PROFILE" ] || die "프로파일을 지정하세요: post-list | view-count | full"

SEED_SRC="$REPO_ROOT/docs/v1/ddl/test-data"
FILES=(01_university_seed.sql 02_member_seed.sql 03_major_seed.sql 04_interest_seed.sql
       05_post_seed.sql 05a_popular_board_post_seed.sql)
[ "$EIGHT_M" = 1 ] && FILES+=(05b_popular_board_post_seed_8m.sql)
[ "$THIRTY_M" = 1 ] && FILES+=(05d_post_seed_30m.sql)
[ "$PROFILE" = "full" ] && FILES+=(06_comment_seed.sql 07_follow_seed.sql 08_block_seed.sql)
RESET_VIEW_COUNT_REDIS=0
if [ "$PROFILE" = "view-count" ] || [ "$PROFILE" = "full" ]; then
  RESET_VIEW_COUNT_REDIS=1
fi

if [ "$DRY_RUN" = 1 ]; then
  echo "Profile: $PROFILE"
  echo "MySQL seed files:"
  printf '  docs/v1/ddl/test-data/%s\n' "${FILES[@]}"
  if [ "$RESET_VIEW_COUNT_REDIS" = 1 ]; then
    echo "Redis reset: enabled"
    "$REPO_ROOT/script/aws/seed/reset-view-count-redis.sh" --dry-run | sed 's/^/  /'
  else
    echo "Redis reset: disabled"
  fi
  exit 0
fi

require_aws
BASTION="$(bastion_ip)"
MYSQL_IP="$(test_out mysql_private_ip)" || die "mysql_private_ip 출력을 얻지 못했습니다"
REDIS_IP=""
if [ "$RESET_VIEW_COUNT_REDIS" = 1 ]; then
  REDIS_IP="$(test_out redis_private_ip)" || die "redis_private_ip 출력을 얻지 못했습니다"
fi

# 이미 돌고 있으면 새로 시작하지 않고 기존 진행에 붙는다 (up.sh 재실행 시 중복 방지)
ALREADY_RUNNING=0
if ssh_bastion 'pgrep -f "bash run-seed.sh"' >/dev/null 2>&1; then
  ALREADY_RUNNING=1
  warn "이미 시딩이 진행 중 — 새로 시작하지 않고 기존 진행을 따라갑니다"
fi

if [ "$ALREADY_RUNNING" = 0 ]; then
# bastion에서 돌릴 러너 생성 (\$ = bastion 실행 시점에 평가)
RUNNER="$(mktemp)"
cat > "$RUNNER" <<EOF
#!/bin/bash
set -uo pipefail
cd /home/ec2-user/seed
command -v mysql >/dev/null || sudo dnf install -y mariadb105 >/dev/null

DB_PASSWORD=\$(aws ssm get-parameter --name $DB_PASSWORD_SSM_KEY --with-decryption \
  --region $AWS_REGION --query Parameter.Value --output text) || { echo "SEED_FAIL ssm"; exit 1; }

for i in \$(seq 1 60); do
  mysql --init-command="SET SESSION time_zone='+09:00'" -h $MYSQL_IP -u $DB_USER -p"\$DB_PASSWORD" -e 'SELECT 1' $DB_NAME >/dev/null 2>&1 && break
  echo "MySQL 대기 중 (\$i/60)"; sleep 10
  [ "\$i" = 60 ] && { echo "SEED_FAIL mysql-unreachable"; exit 1; }
done

if [ "$RESET_VIEW_COUNT_REDIS" = 1 ]; then
  command -v redis-cli >/dev/null || command -v redis6-cli >/dev/null || sudo dnf install -y redis6 >/dev/null
  for i in \$(seq 1 60); do
    REDIS_HOST=$REDIS_IP bash reset-view-count-redis.sh >/dev/null 2>&1 && break
    echo "Redis 대기 중 (\$i/60)"; sleep 5
    [ "\$i" = 60 ] && { echo "SEED_FAIL redis-unreachable"; exit 1; }
  done
  echo "=== 조회수 Redis 초기화 완료 \$(date '+%H:%M:%S')"
fi

for f in ${FILES[*]}; do
  echo "=== \$f 시작 \$(date '+%H:%M:%S')"
  if ! mysql --init-command="SET SESSION time_zone='+09:00'" -h $MYSQL_IP -u $DB_USER -p"\$DB_PASSWORD" $DB_NAME < "\$f"; then
    echo "SEED_FAIL \$f"; exit 1
  fi
done
echo "SEED_DONE \$(date '+%H:%M:%S')"
EOF

log "시드 파일 ${#FILES[@]}개를 bastion($BASTION)으로 전송"
ssh_bastion 'mkdir -p seed'
SCP_FILES=()
for f in "${FILES[@]}"; do SCP_FILES+=("$SEED_SRC/$f"); done
[ "$RESET_VIEW_COUNT_REDIS" = 1 ] && SCP_FILES+=("$REPO_ROOT/script/aws/seed/reset-view-count-redis.sh")
scp "${SSH_OPTS[@]}" -q "${SCP_FILES[@]}" "$RUNNER" ec2-user@"$BASTION":seed/
# (nohup … &) 서브셸 래핑: 러너를 완전히 분리해 ssh 세션이 시딩 종료까지 붙잡히지 않게 한다
ssh_bastion "cd seed && mv $(basename "$RUNNER") run-seed.sh && rm -f seed.log && (nohup bash run-seed.sh >> seed.log 2>&1 < /dev/null &)"
rm -f "$RUNNER"
log "시딩 시작 (프로파일: $PROFILE$([ "$EIGHT_M" = 1 ] && echo ' +8m' || true)$([ "$THIRTY_M" = 1 ] && echo ' +30m' || true), bastion에서 백그라운드 실행)"
fi # ALREADY_RUNNING=0

if [ "$WAIT" = 1 ]; then
  log "완료 대기 중 — 중단해도 시딩은 bastion에서 계속됩니다 (재확인: seed.sh --follow)"
  while :; do
    LAST="$(ssh_bastion 'tail -1 seed/seed.log 2>/dev/null' || true)"
    case "$LAST" in
      SEED_DONE*) echo; log "시드 완료 ($LAST)"; break ;;
      SEED_FAIL*) echo; die "시드 실패 ($LAST) — script/aws/seed.sh --status 로 로그 확인" ;;
    esac
    printf '  %s 진행: %-50s\r' "$(date +%H:%M:%S)" "${LAST:-시작 대기}"
    sleep 20
  done
else
  log "진행 확인: script/aws/seed.sh --follow"
fi
