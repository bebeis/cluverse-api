# base에는 alb_sg만 둔다. 나머지 SG(ecs/db/redis/monitoring/bastion)는 test 스택에서 생성.
# 순환 참조 방지를 위해 규칙은 aws_security_group_rule로 분리한다.
resource "aws_security_group" "alb" {
  name        = "cluverse-alb-sg"
  description = "ALB: HTTP/HTTPS from anywhere"
  vpc_id      = aws_vpc.main.id

  tags = { Name = "cluverse-alb-sg" }
}

resource "aws_security_group_rule" "alb_in_http" {
  type              = "ingress"
  security_group_id = aws_security_group.alb.id
  from_port         = 80
  to_port           = 80
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "HTTP (redirect to HTTPS)"
}

resource "aws_security_group_rule" "alb_in_https" {
  type              = "ingress"
  security_group_id = aws_security_group.alb.id
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "HTTPS"
}

resource "aws_security_group_rule" "alb_out_all" {
  type              = "egress"
  security_group_id = aws_security_group.alb.id
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "ECS dynamic ports (32768-65535) to VPC targets"
}
