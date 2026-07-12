# 계단식 SG — 모든 인바운드는 source SG 참조(공인 IP 예외는 bastion의 my_ip 뿐).
# 상호 참조로 인한 순환을 피하기 위해 SG 본체와 규칙(aws_security_group_rule)을 분리한다.

resource "aws_security_group" "ecs" {
  name        = "cluverse-ecs-sg"
  description = "ECS container instances"
  vpc_id      = local.vpc_id

  tags = { Name = "cluverse-ecs-sg" }
}

resource "aws_security_group" "db" {
  name        = "cluverse-db-sg"
  description = "MySQL EC2"
  vpc_id      = local.vpc_id

  tags = { Name = "cluverse-db-sg" }
}

resource "aws_security_group" "redis" {
  name        = "cluverse-redis-sg"
  description = "Redis EC2"
  vpc_id      = local.vpc_id

  tags = { Name = "cluverse-redis-sg" }
}

resource "aws_security_group" "monitoring" {
  name        = "cluverse-monitoring-sg"
  description = "Prometheus/Grafana EC2"
  vpc_id      = local.vpc_id

  tags = { Name = "cluverse-monitoring-sg" }
}

resource "aws_security_group" "bastion" {
  name        = "cluverse-bastion-sg"
  description = "Bastion host"
  vpc_id      = local.vpc_id

  tags = { Name = "cluverse-bastion-sg" }
}

# ---------- ecs_sg ----------
# bridge + 동적 포트(32768-65535)이므로 ALB에서 동적 포트 범위를 연다.
resource "aws_security_group_rule" "ecs_in_alb_dynamic" {
  type                     = "ingress"
  security_group_id        = aws_security_group.ecs.id
  from_port                = 32768
  to_port                  = 65535
  protocol                 = "tcp"
  source_security_group_id = local.alb_sg_id
  description              = "ALB to ECS dynamic host ports"
}

resource "aws_security_group_rule" "ecs_in_monitoring_node_exporter" {
  type                     = "ingress"
  security_group_id        = aws_security_group.ecs.id
  from_port                = 9100
  to_port                  = 9100
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.monitoring.id
  description              = "Prometheus scrape node_exporter"
}

# 앱 컨테이너의 /actuator/prometheus 도 동적 호스트 포트에 물리므로 범위로 연다.
resource "aws_security_group_rule" "ecs_in_monitoring_app" {
  type                     = "ingress"
  security_group_id        = aws_security_group.ecs.id
  from_port                = 32768
  to_port                  = 65535
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.monitoring.id
  description              = "Prometheus scrape app dynamic ports"
}

resource "aws_security_group_rule" "ecs_out_all" {
  type              = "egress"
  security_group_id = aws_security_group.ecs.id
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "ECR pull, package install via NAT, DB/Redis access"
}

# ---------- db_sg ----------
resource "aws_security_group_rule" "db_in_ecs_mysql" {
  type                     = "ingress"
  security_group_id        = aws_security_group.db.id
  from_port                = 3306
  to_port                  = 3306
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs.id
  description              = "MySQL from ECS"
}

# 로컬 → bastion 터널 → MySQL(3306) 접근 경로. 터널의 소스는 bastion이므로 필요.
resource "aws_security_group_rule" "db_in_bastion_mysql" {
  type                     = "ingress"
  security_group_id        = aws_security_group.db.id
  from_port                = 3306
  to_port                  = 3306
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.bastion.id
  description              = "MySQL via bastion tunnel"
}

resource "aws_security_group_rule" "db_in_monitoring_mysqld_exporter" {
  type                     = "ingress"
  security_group_id        = aws_security_group.db.id
  from_port                = 9104
  to_port                  = 9104
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.monitoring.id
  description              = "Prometheus scrape mysqld_exporter"
}

resource "aws_security_group_rule" "db_in_monitoring_node_exporter" {
  type                     = "ingress"
  security_group_id        = aws_security_group.db.id
  from_port                = 9100
  to_port                  = 9100
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.monitoring.id
  description              = "Prometheus scrape node_exporter"
}

resource "aws_security_group_rule" "db_in_bastion_ssh" {
  type                     = "ingress"
  security_group_id        = aws_security_group.db.id
  from_port                = 22
  to_port                  = 22
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.bastion.id
  description              = "SSH from bastion"
}

resource "aws_security_group_rule" "db_out_all" {
  type              = "egress"
  security_group_id = aws_security_group.db.id
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "package install via NAT"
}

# ---------- redis_sg ----------
resource "aws_security_group_rule" "redis_in_ecs" {
  type                     = "ingress"
  security_group_id        = aws_security_group.redis.id
  from_port                = 6379
  to_port                  = 6379
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs.id
  description              = "Redis from ECS"
}

resource "aws_security_group_rule" "redis_in_bastion" {
  type                     = "ingress"
  security_group_id        = aws_security_group.redis.id
  from_port                = 6379
  to_port                  = 6379
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.bastion.id
  description              = "Redis via bastion tunnel"
}

resource "aws_security_group_rule" "redis_in_monitoring_redis_exporter" {
  type                     = "ingress"
  security_group_id        = aws_security_group.redis.id
  from_port                = 9121
  to_port                  = 9121
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.monitoring.id
  description              = "Prometheus scrape redis_exporter"
}

resource "aws_security_group_rule" "redis_in_monitoring_node_exporter" {
  type                     = "ingress"
  security_group_id        = aws_security_group.redis.id
  from_port                = 9100
  to_port                  = 9100
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.monitoring.id
  description              = "Prometheus scrape node_exporter"
}

resource "aws_security_group_rule" "redis_in_bastion_ssh" {
  type                     = "ingress"
  security_group_id        = aws_security_group.redis.id
  from_port                = 22
  to_port                  = 22
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.bastion.id
  description              = "SSH from bastion"
}

resource "aws_security_group_rule" "redis_out_all" {
  type              = "egress"
  security_group_id = aws_security_group.redis.id
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "package install via NAT"
}

# ---------- monitoring_sg ----------
# 9090(Prometheus)/3000(Grafana)은 bastion 터널로만. 전세계 오픈 금지.
resource "aws_security_group_rule" "monitoring_in_bastion_prometheus" {
  type                     = "ingress"
  security_group_id        = aws_security_group.monitoring.id
  from_port                = 9090
  to_port                  = 9090
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.bastion.id
  description              = "Prometheus UI via bastion tunnel"
}

resource "aws_security_group_rule" "monitoring_in_bastion_grafana" {
  type                     = "ingress"
  security_group_id        = aws_security_group.monitoring.id
  from_port                = 3000
  to_port                  = 3000
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.bastion.id
  description              = "Grafana via bastion tunnel"
}

resource "aws_security_group_rule" "monitoring_in_bastion_ssh" {
  type                     = "ingress"
  security_group_id        = aws_security_group.monitoring.id
  from_port                = 22
  to_port                  = 22
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.bastion.id
  description              = "SSH from bastion"
}

# Prometheus(host 네트워크 컨테이너)가 자기 호스트의 node_exporter를 사설 IP로 self-scrape
resource "aws_security_group_rule" "monitoring_in_self_node_exporter" {
  type              = "ingress"
  security_group_id = aws_security_group.monitoring.id
  from_port         = 9100
  to_port           = 9100
  protocol          = "tcp"
  self              = true
  description       = "Prometheus self-scrape node_exporter"
}

resource "aws_security_group_rule" "monitoring_out_all" {
  type              = "egress"
  security_group_id = aws_security_group.monitoring.id
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "docker pull via NAT, scrape targets"
}

# ---------- bastion_sg ----------
resource "aws_security_group_rule" "bastion_in_ssh" {
  type              = "ingress"
  security_group_id = aws_security_group.bastion.id
  from_port         = 22
  to_port           = 22
  protocol          = "tcp"
  cidr_blocks       = [var.my_ip]
  description       = "SSH from my_ip only"
}

resource "aws_security_group_rule" "bastion_out_all" {
  type              = "egress"
  security_group_id = aws_security_group.bastion.id
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "tunnel targets"
}
