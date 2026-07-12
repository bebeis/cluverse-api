# base 스택 output 참조 방식: terraform_remote_state (local backend).
# base가 S3 backend로 이전하면 여기 backend/config만 맞춰 바꾸면 된다.
data "terraform_remote_state" "base" {
  backend = "local"

  config = {
    path = "${path.module}/../base/terraform.tfstate"
  }
}

data "aws_caller_identity" "current" {}

# 하드코딩 AMI 금지 — SSM 공개 파라미터로 최신 AMI 조회
data "aws_ssm_parameter" "ecs_ami" {
  name = "/aws/service/ecs/optimized-ami/amazon-linux-2023/recommended/image_id"
}

data "aws_ssm_parameter" "al2023_ami" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

locals {
  base = data.terraform_remote_state.base.outputs

  vpc_id             = local.base.vpc_id
  public_subnet_ids  = local.base.public_subnet_ids
  private_subnet_ids = local.base.private_subnet_ids
  alb_sg_id          = local.base.alb_sg_id
  target_group_arn   = local.base.target_group_arn
  ecr_repository_url = local.base.ecr_repository_url

  key_name = var.ssh_public_key != "" ? aws_key_pair.main[0].key_name : null
}

resource "aws_key_pair" "main" {
  count = var.ssh_public_key != "" ? 1 : 0

  key_name   = "cluverse-test-key"
  public_key = var.ssh_public_key
}
