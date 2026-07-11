# DB 자격증명은 SSM Parameter Store에 두고 ECS 태스크가 secrets로 참조한다.
# (태스크 정의 JSON에 평문 비밀번호가 남지 않음)
resource "aws_ssm_parameter" "db_password" {
  name  = "/cluverse/test/db/password"
  type  = "SecureString"
  value = var.db_password

  tags = { Name = "cluverse-db-password" }
}
