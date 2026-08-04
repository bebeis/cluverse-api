#!/usr/bin/env bash
# 로컬 .env의 허용된 키만 AWS SSM Parameter Store SecureString으로 동기화한다.
# .env를 source/eval하지 않으며 시크릿 값은 콘솔에 출력하지 않는다.

source "$(dirname "$0")/lib.sh"

ENV_FILE="$REPO_ROOT/.env"
PARAMETER_PREFIX="/cluverse/test"
DRY_RUN=0

usage() {
  cat <<'EOF'
사용법: script/aws/sync-secrets.sh [옵션]

옵션:
  --env-file PATH  입력할 dotenv 파일 (기본: 리포 루트/.env)
  --prefix PATH    SSM 경로 prefix (기본: /cluverse/test)
  --dry-run        AWS를 변경하지 않고 동기화할 키와 경로만 출력
  -h, --help       도움말
EOF
}

while [ $# -gt 0 ]; do
  case "$1" in
    --env-file)
      ENV_FILE="${2:?--env-file 뒤에 파일 경로가 필요합니다.}"
      shift 2
      ;;
    --prefix)
      PARAMETER_PREFIX="${2:?--prefix 뒤에 SSM 경로가 필요합니다.}"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      usage >&2
      die "알 수 없는 옵션: $1"
      ;;
  esac
done

[ -f "$ENV_FILE" ] || die "dotenv 파일이 없습니다: $ENV_FILE (.env.example을 참고하세요)"
case "$PARAMETER_PREFIX" in
  /*) ;;
  *) die "SSM prefix는 /로 시작해야 합니다: $PARAMETER_PREFIX" ;;
esac
PARAMETER_PREFIX="${PARAMETER_PREFIX%/}"
[ -n "$PARAMETER_PREFIX" ] || die "SSM prefix로 루트(/)를 사용할 수 없습니다."

SECRET_KEYS="
KAKAO_CLIENT_ID
KAKAO_CLIENT_SECRET
KAKAO_REDIRECT_URI
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
GOOGLE_REDIRECT_URI
NAVER_CLIENT_ID
NAVER_CLIENT_SECRET
LOCAL_MAP_SELECTION_TOKEN_SECRET
DATA_GO_KR_SERVICE_KEY
"

parameter_suffix() {
  case "$1" in
    KAKAO_CLIENT_ID) echo "oauth/kakao/client-id" ;;
    KAKAO_CLIENT_SECRET) echo "oauth/kakao/client-secret" ;;
    KAKAO_REDIRECT_URI) echo "oauth/kakao/redirect-uri" ;;
    GOOGLE_CLIENT_ID) echo "oauth/google/client-id" ;;
    GOOGLE_CLIENT_SECRET) echo "oauth/google/client-secret" ;;
    GOOGLE_REDIRECT_URI) echo "oauth/google/redirect-uri" ;;
    NAVER_CLIENT_ID) echo "naver/client-id" ;;
    NAVER_CLIENT_SECRET) echo "naver/client-secret" ;;
    LOCAL_MAP_SELECTION_TOKEN_SECRET) echo "local-map/selection-token-secret" ;;
    DATA_GO_KR_SERVICE_KEY) echo "data-go/service-key" ;;
    *) die "SSM 경로 매핑이 없는 키입니다: $1" ;;
  esac
}

read_env_value() {
  python3 - "$ENV_FILE" "$1" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
target = sys.argv[2]
found = None

for raw_line in path.read_text(encoding="utf-8-sig").splitlines():
    line = raw_line.strip()
    if not line or line.startswith("#"):
        continue
    if line.startswith("export "):
        line = line[7:].lstrip()
    key, separator, value = line.partition("=")
    if not separator or key.strip() != target:
        continue
    value = value.strip()
    if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
        value = value[1:-1]
    found = value

if found is None:
    raise SystemExit(3)
sys.stdout.write(found)
PY
}

MISSING_KEYS=""
for KEY in $SECRET_KEYS; do
  if ! VALUE="$(read_env_value "$KEY")" || [ -z "$VALUE" ]; then
    MISSING_KEYS="$MISSING_KEYS $KEY"
    continue
  fi
  case "$VALUE" in
    replace-me|replace-with-*|*example.com*)
      die "$KEY 값이 .env.example의 placeholder 상태입니다."
      ;;
  esac
  if [ "$KEY" = "LOCAL_MAP_SELECTION_TOKEN_SECRET" ]; then
    BYTE_LENGTH="$(LC_ALL=C printf '%s' "$VALUE" | wc -c | tr -d '[:space:]')"
    [ "$BYTE_LENGTH" -ge 32 ] \
      || die "LOCAL_MAP_SELECTION_TOKEN_SECRET은 32바이트 이상이어야 합니다."
  fi
done
[ -z "$MISSING_KEYS" ] \
  || die "필수 키가 비어 있거나 없습니다:${MISSING_KEYS} (.env.example 참고)"

if [ "$DRY_RUN" = 0 ]; then
  require_aws
fi

PAYLOAD_FILE=""
cleanup_payload() {
  if [ -n "$PAYLOAD_FILE" ] && [ -f "$PAYLOAD_FILE" ]; then
    : > "$PAYLOAD_FILE"
    rm -f "$PAYLOAD_FILE"
  fi
}
trap cleanup_payload EXIT
trap 'exit 130' INT TERM

if [ "$DRY_RUN" = 0 ]; then
  umask 077
  PAYLOAD_FILE="$(mktemp "${TMPDIR:-/tmp}/cluverse-ssm-parameter.XXXXXX.json")"
fi

for KEY in $SECRET_KEYS; do
  VALUE="$(read_env_value "$KEY")"
  PARAMETER_NAME="$PARAMETER_PREFIX/$(parameter_suffix "$KEY")"
  if [ "$DRY_RUN" = 1 ]; then
    log "dry-run: $KEY → $PARAMETER_NAME (SecureString)"
    continue
  fi

  PARAMETER_NAME="$PARAMETER_NAME" PARAMETER_VALUE="$VALUE" PARAMETER_DESCRIPTION="Cluverse ${KEY}" \
    python3 - "$PAYLOAD_FILE" <<'PY'
import json
import os
from pathlib import Path
import sys

payload = {
    "Name": os.environ["PARAMETER_NAME"],
    "Description": os.environ["PARAMETER_DESCRIPTION"],
    "Type": "SecureString",
    "Value": os.environ["PARAMETER_VALUE"],
    "Overwrite": True,
}
Path(sys.argv[1]).write_text(json.dumps(payload), encoding="utf-8")
PY

  aws ssm put-parameter \
    --cli-input-json "file://$PAYLOAD_FILE" \
    --region "$AWS_REGION" \
    --output json >/dev/null
  : > "$PAYLOAD_FILE"
  log "$KEY → $PARAMETER_NAME 동기화 완료"
done

if [ "$DRY_RUN" = 1 ]; then
  log "dry-run 완료 — AWS에는 변경 사항이 없습니다."
else
  log "SSM 시크릿 동기화 완료 — 값은 출력하지 않았습니다."
fi
