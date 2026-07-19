#!/usr/bin/env bash
# script/aws/lib.sh — 공용 헬퍼. 직접 실행하지 말고 다른 스크립트에서 source 한다.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BASE_DIR="$REPO_ROOT/terraform/base"
TEST_DIR="$REPO_ROOT/terraform/test"
SCRIPT_DIR="$REPO_ROOT/script/aws"

# *.tfvars는 .gitignore 대상이라 커밋되지 않는다
SECRETS_FILE="$TEST_DIR/secrets.auto.tfvars"

SSH_KEY="$REPO_ROOT/cluverse-key"
SSH_PUB="$REPO_ROOT/cluverse-key.pub"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
DOMAIN="api.cluverse.cona.team"

# aws CLI와 terraform 모두 이 프로필을 쓴다 (다른 프로필: AWS_PROFILE=xxx ./up.sh)
export AWS_PROFILE="${AWS_PROFILE:-cluverse-terraform}"

ECS_CLUSTER="cluverse-test"
ECS_SERVICE="cluverse-api"
DB_NAME="cluverse_v2"     # terraform/test/variables.tf 기본값과 일치
DB_USER="cluverse_user"
DB_PASSWORD_SSM_KEY="/cluverse/test/db/password"

# bastion은 사이클마다 새 EIP를 받으므로 호스트키 검증은 무의미하다 — 기록하지 않는다
SSH_OPTS=(-i "$SSH_KEY" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null
          -o LogLevel=ERROR -o ServerAliveInterval=30 -o ConnectTimeout=10)

log()  { printf '\033[1;32m[%s]\033[0m %s\n' "$(date +%H:%M:%S)" "$*"; }
warn() { printf '\033[1;33m[%s]\033[0m %s\n' "$(date +%H:%M:%S)" "$*"; }
die()  { printf '\033[1;31m[ERROR]\033[0m %s\n' "$*" >&2; exit 1; }

tf_base()  { terraform -chdir="$BASE_DIR" "$@"; }
tf_test()  { terraform -chdir="$TEST_DIR" "$@"; }
base_out() { tf_base output -raw "$1" 2>/dev/null; }
test_out() { tf_test output -raw "$1" 2>/dev/null; }

tf_init_if_needed() { [ -d "$1/.terraform" ] || terraform -chdir="$1" init; }

require_aws() {
  aws sts get-caller-identity --query Account --output text >/dev/null 2>&1 \
    || die "AWS 자격증명이 유효하지 않습니다 (프로필: $AWS_PROFILE). aws configure --profile $AWS_PROFILE 후 다시 실행하세요."
}

require_base() {
  base_out alb_dns_name >/dev/null \
    || die "base 스택이 아직 없습니다. 최초 1회 script/aws/base-up.sh 를 먼저 실행하세요."
}

my_ip_cidr() {
  local ip
  ip="$(curl -fsS --max-time 10 https://checkip.amazonaws.com | tr -d '[:space:]')"
  case "$ip" in
    *[!0-9.]*|"") die "공인 IP 조회 실패 (checkip.amazonaws.com)" ;;
  esac
  echo "$ip/32"
}

bastion_ip() {
  test_out bastion_public_ip || die "bastion IP를 얻지 못했습니다 — test 스택이 떠 있나요? (script/aws/up.sh)"
}

ssh_bastion() { # 사용: ssh_bastion '원격 명령'
  ssh "${SSH_OPTS[@]}" ec2-user@"$(bastion_ip)" "$@"
}
