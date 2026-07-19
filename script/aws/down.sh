#!/usr/bin/env bash
# 측정 세션 종료 "딸깍" 스크립트.
# test 스택 전체 destroy(NAT/EC2/EIP — 시간당 과금 전부) + 로컬 터널 정리 + 잔존 과금 리소스 점검.
# base(ALB/ACM/ECR/VPC)는 유지된다 — DNS/HTTPS/이미지가 리셋되지 않도록 의도된 동작.
#
# 사용법: script/aws/down.sh
source "$(dirname "$0")/lib.sh"

require_aws

"$SCRIPT_DIR/tunnel.sh" stop >/dev/null 2>&1 || true

log "terraform destroy (test)"
tf_test destroy -auto-approve

# 시간당 과금 리소스 잔존 점검 (혹시 남았으면 수동 정리 대상)
NAT="$(aws ec2 describe-nat-gateways --region "$AWS_REGION" \
  --filter Name=state,Values=pending,available --query 'NatGateways[].NatGatewayId' --output text)"
# 미연결 EIP만 점검 — 연결된 EIP는 소유 리소스(base ALB 등)의 점검에 맡긴다
EIP="$(aws ec2 describe-addresses --region "$AWS_REGION" --query 'Addresses[?AssociationId==null].PublicIp' --output text)"
EC2="$(aws ec2 describe-instances --region "$AWS_REGION" \
  --filters Name=instance-state-name,Values=pending,running Name=tag:Project,Values=cluverse \
  --query 'Reservations[].Instances[].InstanceId' --output text)"

LEFT=0
[ -n "$NAT" ] && { warn "NAT 게이트웨이 잔존: $NAT"; LEFT=1; }
[ -n "$EIP" ] && { warn "EIP 잔존: $EIP"; LEFT=1; }
[ -n "$EC2" ] && { warn "cluverse EC2 잔존: $EC2"; LEFT=1; }
if [ "$LEFT" = 0 ]; then
  log "시간당 과금 리소스(NAT/EIP/EC2) 없음 — 정리 완료"
fi
log "base의 ALB는 유지 중입니다 (시간당 약 \$0.0225 ≈ 월 \$16). 장기 미사용 시 base destroy도 고려"
