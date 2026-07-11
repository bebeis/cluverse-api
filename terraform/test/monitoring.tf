# Prometheus/Grafana EC2 (프라이빗) — docker compose로 구동.
# 접근은 bastion 터널(9090/3000) 또는 SSM 포트포워딩으로만 한다 (outputs.tf 참고).
resource "aws_instance" "monitoring" {
  ami                    = data.aws_ssm_parameter.al2023_ami.value
  instance_type          = var.monitoring_instance_type
  subnet_id              = local.private_subnet_ids[0]
  private_ip             = var.monitoring_private_ip
  vpc_security_group_ids = [aws_security_group.monitoring.id]
  iam_instance_profile   = aws_iam_instance_profile.monitoring.name
  key_name               = local.key_name

  depends_on = [aws_route.private_nat]

  metadata_options {
    http_tokens                 = "required"
    http_put_response_hop_limit = 2 # 컨테이너(Prometheus ec2_sd)의 IMDS 자격증명 접근용
  }

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
  }

  user_data = templatefile("${path.module}/templates/monitoring.sh.tpl", {
    aws_region = var.aws_region
    mysql_ip   = var.mysql_private_ip
    redis_ip   = var.redis_private_ip
  })

  tags = {
    Name         = "cluverse-monitoring"
    NodeExporter = "true"
  }
}
