locals {
  cluster_name = "cluverse-test"
}

resource "aws_ecs_cluster" "main" {
  name = local.cluster_name
}

resource "aws_cloudwatch_log_group" "api" {
  name              = "/ecs/cluverse-api"
  retention_in_days = 3 # 부하 테스트 용도라 짧게
}

resource "aws_launch_template" "ecs" {
  name_prefix   = "cluverse-ecs-"
  image_id      = data.aws_ssm_parameter.ecs_ami.value # ECS-optimized AL2023, SSM으로 최신 조회
  instance_type = var.ecs_instance_type
  key_name      = local.key_name

  vpc_security_group_ids = [aws_security_group.ecs.id]

  iam_instance_profile {
    arn = aws_iam_instance_profile.ecs_instance.arn
  }

  metadata_options {
    http_tokens                 = "required"
    http_put_response_hop_limit = 2 # 컨테이너에서 IMDS 접근(태스크 역할 등) 허용
  }

  user_data = base64encode(templatefile("${path.module}/templates/ecs_node.sh.tpl", {
    cluster_name = local.cluster_name
  }))

  tag_specifications {
    resource_type = "instance"
    tags = {
      Name         = "cluverse-ecs-node"
      Project      = "cluverse"
      ManagedBy    = "terraform"
      Stack        = "test"
      NodeExporter = "true" # Prometheus ec2_sd가 이 태그로 node_exporter 대상 발견
    }
  }
}

resource "aws_autoscaling_group" "ecs" {
  name                = "cluverse-ecs-asg"
  min_size            = 0
  max_size            = 4
  desired_capacity    = var.ecs_desired_count
  vpc_zone_identifier = local.private_subnet_ids

  # 부팅 시 user_data가 인터넷에서 node_exporter를 받으므로 NAT route가 먼저 있어야 한다.
  depends_on = [aws_route.private_nat]

  launch_template {
    id      = aws_launch_template.ecs.id
    version = "$Latest"
  }

  # destroy를 빠르게 (부하 테스트 반복 사이클용)
  force_delete = true

  lifecycle {
    ignore_changes = [desired_capacity] # capacity provider managed scaling이 조정
  }

  tag {
    key                 = "AmazonECSManaged"
    value               = "true"
    propagate_at_launch = true
  }
}

resource "aws_ecs_capacity_provider" "main" {
  name = "cluverse-test-cp"

  auto_scaling_group_provider {
    auto_scaling_group_arn = aws_autoscaling_group.ecs.arn
    # ENABLED면 destroy 시 scale-in 보호가 걸려 오래 걸린다. 테스트용이라 DISABLED.
    managed_termination_protection = "DISABLED"

    managed_scaling {
      status                    = "ENABLED"
      target_capacity           = 100
      minimum_scaling_step_size = 1
      maximum_scaling_step_size = 2
    }
  }
}

resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name       = aws_ecs_cluster.main.name
  capacity_providers = [aws_ecs_capacity_provider.main.name]

  default_capacity_provider_strategy {
    capacity_provider = aws_ecs_capacity_provider.main.name
    weight            = 1
  }
}

# 네트워크 모드: bridge + 고정 호스트 포트 8080.
# base의 target group이 target_type="instance"로 만들어져 있으므로 반드시 bridge여야 한다.
# (awsvpc로 바꾸려면 base의 target_type도 "ip"로 함께 바꿔야 함 — base/alb.tf 주석 참고)
# hostPort 고정 이유: Prometheus가 EC2 SD로 앱 /actuator/prometheus 를 긁으려면 포트가
# 예측 가능해야 한다. 이 스택은 ecs_desired_count가 ASG와 서비스에 같이 걸려 인스턴스당
# 1태스크이므로 고정 포트로 잃는 밀도가 없다. (동적 포트로 되돌리면 앱 메트릭 수집 불가)
resource "aws_ecs_task_definition" "api" {
  family                   = "cluverse-api"
  requires_compatibilities = ["EC2"]
  network_mode             = "bridge"
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name              = "cluverse-api"
      image             = "${local.ecr_repository_url}:${var.container_image_tag}"
      essential         = true
      memoryReservation = 512
      portMappings = [
        { containerPort = 8080, hostPort = 8080, protocol = "tcp" }
      ]
      environment = [
        # Spring relaxed binding — docker-compose.yml과 동일한 관례로 주입
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:mysql://${var.mysql_private_ip}:3306/${var.db_name}?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true" },
        { name = "SPRING_DATASOURCE_USERNAME", value = var.db_username },
        { name = "SPRING_REDIS_HOST", value = var.redis_private_ip },
        { name = "SPRING_REDIS_PORT", value = "6379" },
        { name = "SPRING_DATA_REDIS_HOST", value = var.redis_private_ip },
        { name = "SPRING_DATA_REDIS_PORT", value = "6379" }
      ]
      secrets = [
        { name = "SPRING_DATASOURCE_PASSWORD", valueFrom = aws_ssm_parameter.db_password.arn }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.api.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "api"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "api" {
  name            = "cluverse-api"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.api.arn
  desired_count   = var.ecs_desired_count

  # Spring Boot 부팅이 느려 grace가 짧으면 ALB 헬스체크 실패 → 태스크 무한 재시작.
  # 넉넉하게 180초.
  health_check_grace_period_seconds = 180

  capacity_provider_strategy {
    capacity_provider = aws_ecs_capacity_provider.main.name
    weight            = 1
  }

  load_balancer {
    target_group_arn = local.target_group_arn # base의 빈 target group에 등록
    container_name   = "cluverse-api"
    container_port   = 8080
  }

  # DB/Redis가 준비된 뒤 태스크가 뜨도록 (부팅 직후 커넥션 실패로 인한 재시작 줄임)
  depends_on = [
    aws_ecs_cluster_capacity_providers.main,
    aws_instance.mysql,
    aws_instance.redis,
  ]
}
