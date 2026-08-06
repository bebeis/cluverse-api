#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
VARIABLES_FILE="$ROOT_DIR/terraform/test/variables.tf"
PROMETHEUS_TEMPLATE="$ROOT_DIR/terraform/test/templates/monitoring.sh.tpl"
APP_DASHBOARD="$ROOT_DIR/script/aws/grafana/app-spring.json"

ecs_default_count="$({
  sed -n '/variable "ecs_desired_count" {/,/^}/p' "$VARIABLES_FILE"
} | sed -n 's/^[[:space:]]*default[[:space:]]*=[[:space:]]*\([0-9][0-9]*\).*/\1/p')"

[[ "$ecs_default_count" == "2" ]] || {
  echo "ecs_desired_count 기본값이 2여야 합니다. actual=$ecs_default_count" >&2
  exit 1
}

grep -q 'job_name: spring' "$PROMETHEUS_TEMPLATE"
grep -q 'values: \["cluverse-ecs-node"\]' "$PROMETHEUS_TEMPLATE"
grep -q 'instance-state-name' "$PROMETHEUS_TEMPLATE"

jq -e '
  ([.panels[] | select(.id == 16) | .targets[].expr] | all(contains("by(instance)"))) and
  ([.panels[] | select(.id == 17) | .targets[].legendFormat] | all(contains("{{instance}}"))) and
  ([.panels[] | select(.id == 18) | .targets[].expr] | all(contains("by(instance)"))) and
  ([.panels[] | select(.id == 18) | .targets[].legendFormat] | all(contains("{{instance}}"))) and
  ([.panels[] | select(.id == 19) | .targets[].expr] | all(contains("by(instance)"))) and
  ([.panels[] | select(.id == 19) | .targets[].legendFormat] | all(contains("{{instance}}")))
' "$APP_DASHBOARD" >/dev/null

echo "ECS 2대 및 인스턴스별 Grafana 관측 구성 테스트 통과"
