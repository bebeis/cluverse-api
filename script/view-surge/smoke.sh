#!/usr/bin/env bash
# ============================================================
# view-surge e2e 스모크 — V4가 설계대로 동작하는지 기계적으로 검증한다
# ============================================================
#
# 검증하는 계약:
#   [A] 정합성: 성공(200) 요청 수 == post_view_count 증가량 (최종 드레인 후)
#   [B] 상태 전이: burst → tracking 행 생성 → 버퍼 경로 사용(redis_path 증가)
#       → 플러시(pending 드레인) → 연장(expires_at 뒤로) → 만료 정리(행/키 삭제)
#       → MySQL 직접 경로 복귀
#   [C] 장애 격리: Redis 중단 중에도 에러 0% (MySQL 폴백), 복구 후 자동 재개
#
# 전제: docker compose 스택(db, redis-server, spring-app-1/2, nginx-lb)이 떠 있고
#       Flyway V1 baseline이 적용된 상태(view_surge_tracking 존재).
#       기본 view-surge 설정(threshold 200 / window 10s / grace 15s) 기준.
#
# 사용:
#   script/view-surge/smoke.sh                # 전체 (Redis 장애 주입 포함, 약 3분)
#   script/view-surge/smoke.sh --skip-outage  # 장애 주입 생략 (약 90초)
#
# 주의: Redis 장애 단계에서 redis-server 컨테이너를 stop/start 한다.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
DB_CONTAINER="${DB_CONTAINER:-db}"
REDIS_CONTAINER="${REDIS_CONTAINER:-redis-server}"
APP_CONTAINERS=(${APP_CONTAINERS:-spring-app-1 spring-app-2})
MYSQL_ARGS=(-ucluverse_user -ptest1234 cluverse_v2 -N)
SMOKE_POST_ID="${SMOKE_POST_ID:-900000001}"
PENDING_KEY="view:pending:${SMOKE_POST_ID}"
PARALLEL=16

SKIP_OUTAGE=0
[ "${1:-}" = "--skip-outage" ] && SKIP_OUTAGE=1

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT
TOTAL_OK=0
STEP_NO=0

# log는 stderr로 — burst()의 반환값($(...) 캡처)에 섞이면 안 된다
log()  { printf '\033[36m[smoke]\033[0m %s\n' "$*" >&2; }
pass() { printf '\033[32m  PASS\033[0m %s\n' "$*"; }
fail() { printf '\033[31m  FAIL\033[0m %s\n' "$*"; exit 1; }
step() { STEP_NO=$((STEP_NO + 1)); printf '\n\033[1m[%d] %s\033[0m\n' "$STEP_NO" "$*"; }

sql() { docker exec "$DB_CONTAINER" mysql "${MYSQL_ARGS[@]}" -e "$1" 2>/dev/null; }
rcli() { docker exec "$REDIS_CONTAINER" redis-cli "$@"; }
view_count() { sql "SELECT view_count FROM post_view_count WHERE post_id=${SMOKE_POST_ID};"; }
tracking_count() { sql "SELECT COUNT(*) FROM view_surge_tracking WHERE post_id=${SMOKE_POST_ID};"; }
expires_at() { sql "SELECT expires_at FROM view_surge_tracking WHERE post_id=${SMOKE_POST_ID};"; }

# 두 앱 인스턴스의 카운터 합 (버퍼 경로 사용 증명용)
redis_path_total() {
    local sum=0 value
    for app in "${APP_CONTAINERS[@]}"; do
        # wget 실패(순단 등)가 set -e 로 스크립트 전체를 죽이지 않게 흡수하고 0으로 취급
        value=$(docker exec "$app" wget -qO- localhost:8080/actuator/prometheus 2>/dev/null \
            | awk '/^view_count_redis_path_total/ {print $2}' || true)
        sum=$(python3 -c "print(${sum} + ${value:-0})")
    done
    echo "$sum"
}

# burst <건수> <설명> — 200 응답 수를 TOTAL_OK에 누적하고 비200 수를 BURST_FAILS에 담는다
# ($(...) 캡처는 서브셸이라 TOTAL_OK 누적이 사라진다 — 반드시 직접 호출할 것)
BURST_FAILS=0
burst() {
    local count="$1" label="$2" codes_file="$WORK_DIR/codes-$STEP_NO-$RANDOM.txt"
    seq 1 "$count" | xargs -P "$PARALLEL" -I{} \
        curl -s -o /dev/null -w '%{http_code}\n' -X POST \
        "$BASE_URL/api/v4/posts/$SMOKE_POST_ID/view-count" >> "$codes_file"
    local ok
    ok=$(grep -c '^200$' "$codes_file" || true)
    BURST_FAILS=$((count - ok))
    TOTAL_OK=$((TOTAL_OK + ok))
    log "$label: ${count}건 발사, 200=${ok}, 비200=${BURST_FAILS}"
}

# wait_until <타임아웃초> <설명> <조건 커맨드...>
wait_until() {
    local timeout="$1" label="$2"; shift 2
    local deadline=$((SECONDS + timeout))
    until "$@"; do
        if [ "$SECONDS" -ge "$deadline" ]; then
            fail "$label — ${timeout}초 안에 충족되지 않음"
        fi
        sleep 1
    done
    pass "$label"
}

cond_tracking_exists()  { [ "$(tracking_count)" = "1" ]; }
cond_tracking_removed() { [ "$(tracking_count)" = "0" ]; }
cond_pending_drained()  { local v; v=$(rcli GET "$PENDING_KEY"); [ -z "$v" ] || [ "$v" = "0" ]; }
cond_key_removed()      { [ "$(rcli EXISTS "$PENDING_KEY")" = "0" ]; }

force_expire() {
    # MySQL NOW()는 앱 Clock(Asia/Seoul 고정)과 타임존이 다를 수 있다 —
    # 자기 값 기준 상대 이동이라 어느 존에서도 grace 컷오프를 확실히 지난다
    sql "UPDATE view_surge_tracking SET expires_at = expires_at - INTERVAL 1 DAY WHERE post_id=${SMOKE_POST_ID};"
}

# ------------------------------------------------------------
step "사전 점검 — 컨테이너·스키마"
for c in "$DB_CONTAINER" "$REDIS_CONTAINER" "${APP_CONTAINERS[@]}"; do
    docker ps --format '{{.Names}}' | grep -qx "$c" || fail "컨테이너 미기동: $c (docker compose up 필요)"
done
[ "$(sql "SHOW TABLES LIKE 'view_surge_tracking';")" = "view_surge_tracking" ] \
    || fail "view_surge_tracking 없음 — mysql 볼륨을 지우고 재기동해 V1 baseline을 적용할 것"
pass "컨테이너 5개 + 스키마 확인"

step "스모크 데이터 초기화 (post_id=${SMOKE_POST_ID}, 멱등)"
sql "DELETE FROM view_surge_tracking WHERE post_id=${SMOKE_POST_ID};
     DELETE FROM post_view_count WHERE post_id=${SMOKE_POST_ID};
     DELETE FROM post WHERE post_id=${SMOKE_POST_ID};
     INSERT INTO post (post_id, board_id, member_id, title, content, category)
     VALUES (${SMOKE_POST_ID}, 1, 1, 'view-surge 스모크', '본문', 'INFORMATION');
     INSERT INTO post_view_count (post_id, view_count) VALUES (${SMOKE_POST_ID}, 0);"
# 이전 실행이 추적 중이었으면 앱 라우팅 캐시에 아직 남아 있다 — 전파 주기(3s)를 기다린 뒤 키 정리
log "라우팅 캐시 정리 대기 (전파 3s)"
sleep 5
rcli DEL "$PENDING_KEY" > /dev/null
pass "초기화 완료 (view_count=0, 미추적 상태 보장)"

step "평상시 경로 — 단건 요청은 MySQL 직행"
burst 1 "단건"
[ "$BURST_FAILS" = "0" ] || fail "단건 요청 실패"
[ "$(view_count)" = "1" ] || fail "view_count != 1 (실제: $(view_count))"
[ "$(tracking_count)" = "0" ] || fail "평상시인데 tracking 행이 존재"
pass "MySQL 직행 + 미추적 확인"

step "급상승 유발 — 감지·등록 [B1]"
REDIS_PATH_BEFORE=$(redis_path_total)
# 감지는 조회 요청에 업혀서만 일어난다(온디맨드) — 관측 구간(10s)을 넘겨
# 판정이 나올 때까지 실제 급상승처럼 트래픽을 계속 흘린다
DETECT_DEADLINE=$((SECONDS + 60))
until cond_tracking_exists; do
    [ "$SECONDS" -lt "$DETECT_DEADLINE" ] || fail "급상승 감지 안 됨 — 60초 동안 트래픽을 흘렸는데 tracking 행이 없음"
    burst 500 "감지 유발 burst"
    [ "$BURST_FAILS" = "0" ] || fail "감지 유발 burst에 비200 ${BURST_FAILS}건"
done
pass "view_surge_tracking 행 생성(급상승 감지)"
EXPIRES_1=$(expires_at)
log "activated: expires_at=${EXPIRES_1}"

step "경로 전환 — 버퍼 경로 사용 [B2]"
sleep 4   # 라우팅 캐시 전파(3s)
burst 2000 "2차 burst"
[ "$BURST_FAILS" = "0" ] || fail "2차 burst에 비200 ${BURST_FAILS}건"
REDIS_PATH_AFTER=$(redis_path_total)
python3 -c "exit(0 if ${REDIS_PATH_AFTER} > ${REDIS_PATH_BEFORE} else 1)" \
    || fail "redis_path 카운터가 늘지 않음 — 버퍼 경로 미사용 (before=${REDIS_PATH_BEFORE}, after=${REDIS_PATH_AFTER})"
pass "버퍼 경로 사용 확인 (redis_path +$(python3 -c "print(int(${REDIS_PATH_AFTER}-${REDIS_PATH_BEFORE}))"))"

step "플러시·연장 [B3][B4]"
wait_until 20 "pending 드레인(플러시 동작)" cond_pending_drained
EXPIRES_2=$(expires_at)
[ "$EXPIRES_2" \> "$EXPIRES_1" ] || fail "expires_at 미연장 (${EXPIRES_1} → ${EXPIRES_2})"
pass "연장 확인 (${EXPIRES_1} → ${EXPIRES_2})"

step "중간 정합성 [A]"
wait_until 15 "정합성: view_count(=$(view_count)) == 성공 ${TOTAL_OK}건" \
    bash -c "[ \"\$(docker exec $DB_CONTAINER mysql ${MYSQL_ARGS[*]} -e 'SELECT view_count FROM post_view_count WHERE post_id='${SMOKE_POST_ID}';' 2>/dev/null)\" = \"$TOTAL_OK\" ]"

if [ "$SKIP_OUTAGE" = "0" ]; then
    step "Redis 장애 주입 — 폴백 [C]"
    docker stop "$REDIS_CONTAINER" > /dev/null
    log "redis 중단됨 — 추적 중인 글에 burst"
    burst 1000 "장애 중 burst"
    docker start "$REDIS_CONTAINER" > /dev/null
    [ "$BURST_FAILS" = "0" ] || fail "Redis 장애 중 비200 ${BURST_FAILS}건 — 폴백이 요청을 못 지켰음"
    pass "장애 중 에러 0% (MySQL 폴백)"
    log "redis 재시작 — 연결 회복 대기"
    sleep 5
fi

step "만료 정리 — 최종 플러시·키/행 삭제 [B5]"
force_expire
wait_until 40 "tracking 행 삭제" cond_tracking_removed
wait_until 10 "Redis 키 삭제" cond_key_removed

step "최종 정합성 + 직접 경로 복귀 [A][B6]"
wait_until 15 "최종 정합성: view_count == 성공 ${TOTAL_OK}건" \
    bash -c "[ \"\$(docker exec $DB_CONTAINER mysql ${MYSQL_ARGS[*]} -e 'SELECT view_count FROM post_view_count WHERE post_id='${SMOKE_POST_ID}';' 2>/dev/null)\" = \"$TOTAL_OK\" ]"
BEFORE_RETURN=$(view_count)
burst 1 "복귀 단건"
[ "$BURST_FAILS" = "0" ] || fail "복귀 단건 실패"
[ "$(view_count)" = "$((BEFORE_RETURN + 1))" ] || fail "복귀 후 MySQL 직접 증가 안 됨"
pass "직접 경로 복귀 확인"
# 감지 샘플의 관측 구간(3×window=30s)이 아직 안 지났으면 버퍼 기간의 증가량이
# delta로 잡혀 재진입할 수 있다 — 오탐이 아니라 설계된 재진입 흐름이다
if [ "$(tracking_count)" != "0" ]; then
    log "복귀 단건이 재진입을 유발함 (관측 구간 내 재감지 — 설계된 동작). 정리하고 종료"
    force_expire
fi

printf '\n\033[32m✔ 스모크 전체 통과\033[0m — 성공 요청 %d건 전부 유실·중복 없이 반영됨 (post_id=%s)\n' \
    "$TOTAL_OK" "$SMOKE_POST_ID"
