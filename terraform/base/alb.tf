resource "aws_lb" "main" {
  name               = "cluverse-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  tags = { Name = "cluverse-alb" }
}

# 네트워크 모드 합의(base/test 공통 전제):
#   test의 ECS는 bridge 모드 + 동적 호스트 포트(hostPort=0)를 쓴다.
#   → target_type은 반드시 "instance" (awsvpc 모드였다면 "ip"여야 함).
#   bridge+동적포트 선택 이유: t계열 인스턴스의 ENI 개수 제한 없이 인스턴스당 여러 태스크를
#   올릴 수 있고, 부하 테스트에서 인스턴스 수보다 태스크 수를 유연하게 늘리기 좋다.
#   트레이드오프: awsvpc는 태스크별 SG/IP가 생겨 관측이 깔끔하지만 ENI 제한으로 밀도가 낮다.
resource "aws_lb_target_group" "api" {
  name        = "cluverse-api-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "instance"

  # 부하 테스트 사이클을 빠르게 돌리기 위해 드레이닝을 짧게
  deregistration_delay = 30

  health_check {
    path                = "/actuator/health"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = { Name = "cluverse-api-tg" }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

# 443 리스너는 인증서가 ISSUED 된 후에만 만들 수 있다 (variables.tf의 certificate_validated 참고).
resource "aws_lb_listener" "https" {
  count = var.certificate_validated ? 1 : 0

  load_balancer_arn = aws_lb.main.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = aws_acm_certificate.api.arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }
}
