#!/usr/bin/env bash
# script/aws/grafana/*.json 대시보드를 Grafana에 프로비저닝한다 (Cluverse 폴더, 덮어쓰기).
# bastion 경유 임시 SSH 터널로 Grafana API에 접근하므로 tunnel.sh와 독립적으로 동작.
# 멱등: 언제든 재실행 가능. test 스택 재생성 후에도 이걸로 복원하면 된다.
#
# 사용법: script/aws/grafana-dashboards.sh
source "$(dirname "$0")/lib.sh"

MON_IP="$(test_out monitoring_private_ip)" || die "monitoring IP 조회 실패 — test 스택이 떠 있나요?"
BASTION="$(bastion_ip)"
LPORT="${LOCAL_GRAFANA_PROVISION_PORT:-13000}"
SOCK="$SCRIPT_DIR/.grafana-provision.sock"
GRAFANA="http://127.0.0.1:$LPORT"
AUTH=(-u admin:admin)

cleanup() { [ -S "$SOCK" ] && ssh -S "$SOCK" -O exit x 2>/dev/null || true; rm -f "$SOCK"; }
trap cleanup EXIT

ssh "${SSH_OPTS[@]}" -M -S "$SOCK" -f -N -o ExitOnForwardFailure=yes \
  -L "$LPORT:$MON_IP:3000" ec2-user@"$BASTION"

log "Grafana 기동 대기"
for i in $(seq 1 30); do
  curl -fsS --max-time 3 "$GRAFANA/api/health" >/dev/null 2>&1 && break
  [ "$i" = 30 ] && die "Grafana 응답 없음 — 모니터링 인스턴스 user_data가 아직 진행 중일 수 있습니다"
  sleep 5
done

# 폴더 생성 (이미 있으면 409 — 무시)
curl -fsS "${AUTH[@]}" -X POST "$GRAFANA/api/folders" \
  -H 'Content-Type: application/json' \
  -d '{"uid":"cluverse","title":"Cluverse"}' >/dev/null 2>&1 || true

for f in "$SCRIPT_DIR"/grafana/*.json; do
  BODY="$(jq -c '{dashboard: (. + {id: null}), folderUid: "cluverse", overwrite: true}' "$f")"
  RESULT="$(curl -fsS "${AUTH[@]}" -X POST "$GRAFANA/api/dashboards/db" \
    -H 'Content-Type: application/json' -d "$BODY")" \
    || die "$(basename "$f") 업로드 실패"
  log "$(basename "$f") → $(echo "$RESULT" | jq -r .url)"
done
log "완료 — tunnel.sh start 후 http://localhost:3000 의 Cluverse 폴더에서 확인"
