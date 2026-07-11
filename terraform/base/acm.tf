# 도메인이 Route53이 아니라 Spaceship에 있으므로 aws_acm_certificate_validation을 쓰지 않는다.
# (검증 레코드를 자동으로 만들 수 없어 apply가 무한 대기하게 됨)
# 대신 domain_validation_options를 output(acm_validation_records)으로 뽑아
# 사용자가 Spaceship DNS에 CNAME을 수동 입력한다. 발급 확인 후 certificate_validated=true로 재-apply.
resource "aws_acm_certificate" "api" {
  domain_name       = var.domain_name
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = { Name = "cluverse-api-cert" }
}
