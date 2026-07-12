# Bastion (퍼블릭, EIP) — SSH는 var.my_ip에서만.
# 로컬 Grafana/클라이언트 → SSH 터널 → 프라이빗 Prometheus(9090)/MySQL(3306)/Redis(6379).
#
# 권장 대안(SSM Session Manager): 22 오픈이나 공인 IP 없이도 포트포워딩이 가능하다.
#   aws ssm start-session \
#     --target <instance-id> \
#     --document-name AWS-StartPortForwardingSessionToRemoteHost \
#     --parameters '{"host":["10.0.11.30"],"portNumber":["9090"],"localPortNumber":["9090"]}'
# 모든 인스턴스에 AmazonSSMManagedInstanceCore가 붙어 있어 그대로 사용 가능 (outputs.tf에도 예시).
resource "aws_instance" "bastion" {
  ami                         = data.aws_ssm_parameter.al2023_ami.value
  instance_type               = "t3.micro"
  subnet_id                   = local.public_subnet_ids[0]
  associate_public_ip_address = true
  vpc_security_group_ids      = [aws_security_group.bastion.id]
  iam_instance_profile        = aws_iam_instance_profile.node.name
  key_name                    = local.key_name

  metadata_options {
    http_tokens = "required"
  }

  tags = { Name = "cluverse-bastion" }
}

resource "aws_eip" "bastion" {
  instance = aws_instance.bastion.id
  domain   = "vpc"

  tags = { Name = "cluverse-bastion-eip" }
}
