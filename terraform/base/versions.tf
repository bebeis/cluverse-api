terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # state는 로컬 파일로 관리한다.
  # test 스택이 ../base/terraform.tfstate 경로를 terraform_remote_state(local)로 읽으므로,
  # S3 backend 등으로 이전할 경우 test/data.tf의 remote_state 설정도 함께 바꿔야 한다.
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project   = "cluverse"
      ManagedBy = "terraform"
      Stack     = "base"
    }
  }
}
