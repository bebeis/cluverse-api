# ECS 관련 IAM 역할 3개는 용도가 다르므로 반드시 분리한다:
#   (a) ecs_instance  — EC2(컨테이너 인스턴스) 자체의 역할. ECS agent 등록, ECR pull, SSM, CloudWatch agent.
#   (b) task_execution — ECS가 태스크를 "띄울 때" 쓰는 역할. 이미지 pull, awslogs 로그 전송, SSM 파라미터(secrets) 읽기.
#   (c) task           — 컨테이너 "안의 앱"이 AWS API를 부를 때 쓰는 역할. (현재 앱은 S3 등 미사용 → 빈 역할)

data "aws_iam_policy_document" "ec2_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "ecs_tasks_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

# ---------- (a) ECS 컨테이너 인스턴스 역할 ----------
resource "aws_iam_role" "ecs_instance" {
  name               = "cluverse-test-ecs-instance"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume.json
}

resource "aws_iam_role_policy_attachment" "ecs_instance_ecs" {
  role       = aws_iam_role.ecs_instance.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
}

resource "aws_iam_role_policy_attachment" "ecs_instance_ssm" {
  role       = aws_iam_role.ecs_instance.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy_attachment" "ecs_instance_cloudwatch" {
  role       = aws_iam_role.ecs_instance.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

resource "aws_iam_instance_profile" "ecs_instance" {
  name = "cluverse-test-ecs-instance"
  role = aws_iam_role.ecs_instance.name
}

# ---------- (b) Task Execution Role ----------
resource "aws_iam_role" "task_execution" {
  name               = "cluverse-test-task-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
}

resource "aws_iam_role_policy_attachment" "task_execution_managed" {
  role       = aws_iam_role.task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# 컨테이너 secrets(SSM Parameter Store) 읽기 권한
resource "aws_iam_role_policy" "task_execution_ssm_params" {
  name = "read-cluverse-ssm-params"
  role = aws_iam_role.task_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["ssm:GetParameters"]
      Resource = "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter/cluverse/test/*"
    }]
  })
}

# ---------- (c) Task Role (앱용, 현재 빈 역할) ----------
resource "aws_iam_role" "task" {
  name               = "cluverse-test-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
}

# ---------- 모니터링 EC2 역할 (Prometheus ec2_sd용 DescribeInstances + SSM) ----------
resource "aws_iam_role" "monitoring" {
  name               = "cluverse-test-monitoring"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume.json
}

resource "aws_iam_role_policy_attachment" "monitoring_ssm" {
  role       = aws_iam_role.monitoring.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy" "monitoring_ec2_discovery" {
  name = "prometheus-ec2-sd"
  role = aws_iam_role.monitoring.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["ec2:DescribeInstances", "ec2:DescribeAvailabilityZones"]
      Resource = "*"
    }]
  })
}

resource "aws_iam_instance_profile" "monitoring" {
  name = "cluverse-test-monitoring"
  role = aws_iam_role.monitoring.name
}

# ---------- 일반 노드(MySQL/Redis/Bastion) 공통 역할 — SSM 접근용 ----------
resource "aws_iam_role" "node" {
  name               = "cluverse-test-node"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume.json
}

resource "aws_iam_role_policy_attachment" "node_ssm" {
  role       = aws_iam_role.node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "node" {
  name = "cluverse-test-node"
  role = aws_iam_role.node.name
}
