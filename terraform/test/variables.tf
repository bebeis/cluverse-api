variable "aws_region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "my_ip" {
  description = "Bastion SSH를 허용할 내 공인 IP (CIDR, 예: 1.2.3.4/32)"
  type        = string

  validation {
    condition     = can(cidrnetmask(var.my_ip))
    error_message = "my_ip는 CIDR 표기여야 합니다 (예: 1.2.3.4/32)."
  }
}

variable "db_password" {
  description = "MySQL 비밀번호 (root/앱유저/exporter 공용). MySQL validate_password 정책은 LOW로 낮춰두지만 8자 이상 권장."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.db_password) >= 8
    error_message = "db_password는 8자 이상이어야 합니다."
  }

  # user_data 스크립트가 비밀번호를 SQL 리터럴/my.cnf에 그대로 넣으므로,
  # 이스케이프 처리 대신 영숫자로 제한한다 (테스트 스택 전용 비밀번호)
  validation {
    condition     = can(regex("^[A-Za-z0-9]+$", var.db_password))
    error_message = "db_password는 영문자와 숫자만 사용할 수 있습니다."
  }
}

variable "db_name" {
  description = "애플리케이션 DB 이름"
  type        = string
  default     = "cluverse_v2" # src/main/resources/application.yml 의 DB 이름과 일치
}

variable "db_username" {
  description = "애플리케이션 DB 사용자"
  type        = string
  default     = "cluverse_user"
}

variable "ecs_desired_count" {
  description = "ECS 서비스 desired task 수"
  type        = number
  default     = 1
}

variable "ecs_instance_type" {
  description = "ECS 컨테이너 인스턴스 타입"
  type        = string
  default     = "t3.small"
}

variable "db_instance_type" {
  description = "MySQL EC2 인스턴스 타입"
  type        = string
  default     = "t3.small"
}

variable "redis_instance_type" {
  description = "Redis EC2 인스턴스 타입"
  type        = string
  default     = "t3.micro"
}

variable "monitoring_instance_type" {
  description = "Prometheus/Grafana EC2 인스턴스 타입"
  type        = string
  default     = "t3.small"
}

variable "container_image_tag" {
  description = "ECR cluverse-api 이미지 태그"
  type        = string
  default     = "latest"
}

variable "ssh_public_key" {
  description = "인스턴스에 심을 SSH 공개키. 빈 문자열이면 키페어 없이 생성(SSM 접근만 가능)."
  type        = string
  default     = ""
}

# 고정 사설 IP — base의 첫 번째 프라이빗 서브넷(10.0.11.0/24) 대역 안이어야 한다.
variable "mysql_private_ip" {
  description = "MySQL EC2 고정 사설 IP"
  type        = string
  default     = "10.0.11.10"
}

variable "redis_private_ip" {
  description = "Redis EC2 고정 사설 IP"
  type        = string
  default     = "10.0.11.20"
}

variable "monitoring_private_ip" {
  description = "Prometheus/Grafana EC2 고정 사설 IP"
  type        = string
  default     = "10.0.11.30"
}
