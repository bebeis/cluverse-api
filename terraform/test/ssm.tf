# DB 자격증명은 SSM Parameter Store에 두고 ECS 태스크가 secrets로 참조한다.
# (태스크 정의 JSON에 평문 비밀번호가 남지 않음)
resource "aws_ssm_parameter" "db_password" {
  name  = "/cluverse/test/db/password"
  type  = "SecureString"
  value = var.db_password

  tags = { Name = "cluverse-db-password" }
}

# OAuth/네이버/data.go.kr/토큰 서명 키는 로컬 값을 Terraform state에 남기지 않기 위해
# script/aws/sync-secrets.sh가 /cluverse/test 하위 SecureString으로 직접 동기화한다.
# ECS task definition은 이름이 고정된 이 외부 파라미터의 ARN만 참조한다.
