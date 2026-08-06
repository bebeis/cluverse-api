#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
TERRAFORM_BIN="${TERRAFORM_BIN:-terraform}"

terraform_version="$($TERRAFORM_BIN version -json | sed -n 's/.*"terraform_version"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"

if [[ -z "$terraform_version" ]]; then
  echo "Terraform 버전을 확인하지 못했습니다." >&2
  exit 1
fi

echo "Terraform ${terraform_version}로 test 스택 구성을 검증합니다."
"$TERRAFORM_BIN" -chdir="$ROOT_DIR/terraform/test" validate
