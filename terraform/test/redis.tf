# Redis 전용 EC2 — MySQL과 별도 인스턴스로 분리.
# 부하 시 DB와 캐시의 자원(CPU/메모리/네트워크) 경합을 분리해 측정하기 위함.
resource "aws_instance" "redis" {
  ami                    = data.aws_ssm_parameter.al2023_ami.value
  instance_type          = var.redis_instance_type
  subnet_id              = local.private_subnet_ids[0]
  private_ip             = var.redis_private_ip
  vpc_security_group_ids = [aws_security_group.redis.id]
  iam_instance_profile   = aws_iam_instance_profile.node.name
  key_name               = local.key_name

  depends_on = [aws_route.private_nat]

  metadata_options {
    http_tokens = "required"
  }

  user_data = templatefile("${path.module}/templates/redis.sh.tpl", {})

  tags = {
    Name         = "cluverse-redis"
    NodeExporter = "true"
  }
}
