#!/usr/bin/env bash
# 최초 1회: base 스택(VPC/ALB/ACM/ECR) 생성.
# 유일한 수동 단계인 Spaceship DNS 입력 2건(ACM 검증 CNAME, api 도메인 CNAME)을
# 안내하고, 나머지(발급 폴링 → 443 리스너 생성 → HTTPS 도달 확인)는 자동으로 진행한다.
#
# 사용법: script/aws/base-up.sh
source "$(dirname "$0")/lib.sh"

require_aws
tf_init_if_needed "$BASE_DIR"

log "terraform apply (base) — VPC/ALB/ACM/ECR 생성"
tf_base apply -auto-approve

CERT_ARN="$(base_out acm_certificate_arn)"
cert_status() {
  aws acm describe-certificate --certificate-arn "$CERT_ARN" --region "$AWS_REGION" \
    --query 'Certificate.Status' --output text
}

if [ "$(cert_status)" != "ISSUED" ]; then
  echo
  warn "━━━ 수동 단계 1/2: Spaceship DNS에 ACM 검증 CNAME 입력 ━━━"
  warn "아래 name에서 도메인 뒷부분(.cona.team.)을 뺀 호스트 부분을 Host에, value를 Value에:"
  tf_base output acm_validation_records
  echo
  log "입력을 마치면 그대로 두세요 — 발급(ISSUED)될 때까지 30초 간격으로 폴링합니다 (보통 수 분, 최대 30분)"
  DEADLINE=$((SECONDS + 2700))
  while [ "$(cert_status)" != "ISSUED" ]; do
    [ $SECONDS -ge $DEADLINE ] && die "45분 내 미발급 — Spaceship 레코드 입력값을 확인하세요"
    printf '  %s 인증서 상태: %s\r' "$(date +%H:%M:%S)" "$(cert_status)"
    sleep 30
  done
  echo
fi
log "ACM 인증서 ISSUED"

# 이후 어떤 apply에서도 443 리스너가 유지되도록 var를 auto.tfvars로 고정
# (*.tfvars는 gitignore라 리포를 새로 받으면 이 파일을 다시 만들어야 한다 — base-up.sh 재실행이면 충분)
echo 'certificate_validated = true' > "$BASE_DIR/validated.auto.tfvars"
log "443 리스너 생성 (certificate_validated=true)"
tf_base apply -auto-approve

ALB_DNS="$(base_out alb_dns_name)"
echo
warn "━━━ 수동 단계 2/2: Spaceship DNS에 api 도메인 CNAME 입력 ━━━"
warn "  Host: api (도메인 $DOMAIN)  →  Value: $ALB_DNS"
echo
log "입력을 마치면 그대로 두세요 — HTTPS 도달을 폴링합니다 (DNS 전파까지 수 분)"
DEADLINE=$((SECONDS + 1800))
# 로컬 OS의 네거티브 DNS 캐시(레코드 생성 전의 NXDOMAIN, TTL 최대 1시간)를 우회하기 위해
# 공용 DNS로 IP를 직접 얻어 --resolve로 검사한다.
# 타깃이 없으면 ALB가 503을 주는데, TLS 응답이 온 것 자체가 성공이다.
until IP="$(dig +short "$DOMAIN" @8.8.8.8 | grep -m1 -E '^[0-9.]+$')" \
      && [ -n "$IP" ] \
      && curl -so /dev/null --max-time 5 --resolve "$DOMAIN:443:$IP" "https://$DOMAIN/"; do
  [ $SECONDS -ge $DEADLINE ] && die "30분 내 HTTPS 미도달 — CNAME 입력값/전파를 확인하세요"
  printf '  %s https://%s 대기 중…\r' "$(date +%H:%M:%S)" "$DOMAIN"
  sleep 20
done
echo
log "base 스택 완료 — https://$DOMAIN 이 ALB에 연결되었습니다 (지금은 타깃이 없어 503이 정상)"
log "다음: script/aws/up.sh 로 test 스택을 올리세요"
