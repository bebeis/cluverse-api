#!/usr/bin/env bash
# bastion 경유 SSH 터널 시작/중지.
#   Grafana 3000 / Prometheus 9090 / MySQL 3306 / Redis 6379 (로컬 포트는 환경변수로 변경 가능)
#
# 사용법:
#   script/aws/tunnel.sh start
#   script/aws/tunnel.sh stop
#   LOCAL_MYSQL_PORT=13306 script/aws/tunnel.sh start   # 로컬 3306이 이미 사용 중일 때
source "$(dirname "$0")/lib.sh"

SOCK="$SCRIPT_DIR/.tunnel.sock"
CMD="${1:-start}"

case "$CMD" in
  start)
    [ -S "$SOCK" ] && die "터널이 이미 떠 있습니다 (script/aws/tunnel.sh stop 후 재시작)"
    BASTION="$(bastion_ip)"
    MON="$(test_out monitoring_private_ip)"
    MYSQL="$(test_out mysql_private_ip)"
    REDIS="$(test_out redis_private_ip)"
    ssh "${SSH_OPTS[@]}" -M -S "$SOCK" -f -N -o ExitOnForwardFailure=yes \
      -L "${LOCAL_GRAFANA_PORT:-3000}:$MON:3000" \
      -L "${LOCAL_PROM_PORT:-9090}:$MON:9090" \
      -L "${LOCAL_MYSQL_PORT:-3306}:$MYSQL:3306" \
      -L "${LOCAL_REDIS_PORT:-6379}:$REDIS:6379" \
      ec2-user@"$BASTION"
    log "터널 시작 — Grafana http://localhost:${LOCAL_GRAFANA_PORT:-3000} (admin/admin), Prometheus :${LOCAL_PROM_PORT:-9090}, MySQL :${LOCAL_MYSQL_PORT:-3306}, Redis :${LOCAL_REDIS_PORT:-6379}"
    log "중지: script/aws/tunnel.sh stop"
    ;;
  stop)
    if [ -S "$SOCK" ]; then
      ssh -S "$SOCK" -O exit bastion 2>/dev/null || true
      rm -f "$SOCK"
      log "터널 중지"
    else
      log "떠 있는 터널 없음"
    fi
    ;;
  *) die "사용법: tunnel.sh start|stop" ;;
esac
