# MySQL 전용 EC2 (프라이빗, 고정 사설 IP).
# EC2 직접 설치라 destroy 시 데이터가 휘발되지만, 부하 테스트에선 매번 클린 상태가 오히려 유리.
resource "aws_instance" "mysql" {
  ami                    = data.aws_ssm_parameter.al2023_ami.value
  instance_type          = var.db_instance_type
  subnet_id              = local.private_subnet_ids[0]
  private_ip             = var.mysql_private_ip
  vpc_security_group_ids = [aws_security_group.db.id]
  iam_instance_profile   = aws_iam_instance_profile.node.name
  key_name               = local.key_name

  # user_data가 MySQL yum repo/exporter를 인터넷에서 받으므로 NAT route 선행 필요
  depends_on = [aws_route.private_nat]

  metadata_options {
    http_tokens = "required"
  }

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  # 비밀번호는 user_data에 평문으로 싣지 않고, 부팅 시 SSM Parameter Store에서 조회한다
  user_data = templatefile("${path.module}/templates/mysql.sh.tpl", {
    db_name             = var.db_name
    db_username         = var.db_username
    db_password_ssm_key = aws_ssm_parameter.db_password.name
    aws_region          = var.aws_region
  })

  tags = {
    Name         = "cluverse-mysql"
    NodeExporter = "true"
  }
}
