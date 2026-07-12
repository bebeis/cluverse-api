variable "aws_region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "domain_name" {
  description = "API 도메인 (Spaceship 등록, Route53 아님)"
  type        = string
  default     = "api.cluverse.cona.team"
}

# ACM 인증서는 Spaceship에 CNAME을 수동 입력해야 발급(ISSUED)된다.
# 발급 전에는 ALB 443 리스너를 만들 수 없으므로(pending 인증서는 attach 불가),
# 1차 apply(false) → Spaceship 검증 → 발급 확인 → -var certificate_validated=true 재-apply 순서로 진행한다.
variable "certificate_validated" {
  description = "ACM 인증서가 ISSUED 상태가 된 후 true로 바꿔 443 리스너를 생성"
  type        = bool
  default     = false
}
