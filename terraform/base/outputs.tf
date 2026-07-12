output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "퍼블릭 서브넷 ID 목록 (2 AZ)"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "프라이빗 서브넷 ID 목록 (2 AZ)"
  value       = aws_subnet.private[*].id
}

output "private_subnet_cidrs" {
  description = "프라이빗 서브넷 CIDR (test의 고정 private_ip가 이 대역 안이어야 함)"
  value       = aws_subnet.private[*].cidr_block
}

output "private_route_table_id" {
  description = "빈 프라이빗 라우팅 테이블 ID — test가 NAT egress route를 여기에 추가"
  value       = aws_route_table.private.id
}

output "alb_dns_name" {
  description = "ALB DNS 이름 — Spaceship에서 api.cluverse.cona.team CNAME 대상으로 입력"
  value       = aws_lb.main.dns_name
}

output "alb_sg_id" {
  description = "ALB 보안그룹 ID — test의 ecs_sg가 source SG로 참조"
  value       = aws_security_group.alb.id
}

output "target_group_arn" {
  description = "빈 Target Group ARN — test의 ECS 서비스가 여기에 등록"
  value       = aws_lb_target_group.api.arn
}

output "acm_certificate_arn" {
  description = "ACM 인증서 ARN"
  value       = aws_acm_certificate.api.arn
}

# Spaceship DNS에 입력할 검증 레코드.
# name에서 도메인 부분을 뺀 호스트 부분만 Spaceship의 Host 칸에 넣으면 된다.
output "acm_validation_records" {
  description = "Spaceship에 수동 입력할 ACM DNS 검증 CNAME 레코드"
  value = [
    for dvo in aws_acm_certificate.api.domain_validation_options : {
      name  = dvo.resource_record_name
      type  = dvo.resource_record_type
      value = dvo.resource_record_value
    }
  ]
}

output "ecr_repository_url" {
  description = "ECR 리포지토리 URL (docker push 대상)"
  value       = aws_ecr_repository.api.repository_url
}
