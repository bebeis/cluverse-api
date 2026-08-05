#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
GRAFANA_URL="${GRAFANA_URL:-http://localhost:3000}"
GRAFANA_USER="${GRAFANA_USER:-admin}"
GRAFANA_PASSWORD="${GRAFANA_PASSWORD:-admin}"
DASHBOARD="$SCRIPT_DIR/spike-traffic-dashboard.json"

command -v curl >/dev/null || { echo "curl이 필요합니다." >&2; exit 1; }
command -v jq >/dev/null || { echo "jq가 필요합니다." >&2; exit 1; }

curl -fsS -u "$GRAFANA_USER:$GRAFANA_PASSWORD" \
  -X POST "$GRAFANA_URL/api/folders" \
  -H 'Content-Type: application/json' \
  -d '{"uid":"cluverse","title":"Cluverse"}' >/dev/null 2>&1 || true

BODY="$(jq -c '{dashboard: (. + {id: null}), folderUid: "cluverse", overwrite: true}' "$DASHBOARD")"
RESULT="$(curl -fsS -u "$GRAFANA_USER:$GRAFANA_PASSWORD" \
  -X POST "$GRAFANA_URL/api/dashboards/db" \
  -H 'Content-Type: application/json' \
  -d "$BODY")"
echo "$GRAFANA_URL$(printf '%s' "$RESULT" | jq -r .url)"
