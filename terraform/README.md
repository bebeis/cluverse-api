# Cluverse 부하 테스트 인프라 (Terraform, base / test 2-스택)

`apply`/`destroy`를 반복해도 DNS·HTTPS가 리셋되지 않도록 state를 두 개로 분리한다.

| 스택 | 수명 | 내용 |
|------|------|------|
| `base/` | 한 번만 apply, destroy 안 함 | VPC/서브넷/IGW/라우팅(프라이빗 RT는 빈 채로), ALB+리스너+빈 TG, ACM 인증서, alb_sg, ECR |
| `test/` | apply/destroy 반복 | NAT(+프라이빗 egress route), ECS on EC2, MySQL EC2, Redis EC2, Prometheus/Grafana EC2, Bastion, SG/IAM/SSM |

- **base → test 참조**: `test/data.tf`의 `terraform_remote_state`(local backend, `../base/terraform.tfstate`). base를 S3 backend로 옮기면 이 설정도 함께 수정.
- **네트워크 모드 합의**: ECS는 bridge + 동적 호스트 포트 → base의 target group은 `target_type = "instance"`. awsvpc로 바꾸려면 양쪽을 함께 바꿔야 한다 (`base/alb.tf` 주석).
- ALB DNS 이름, ACM 검증 상태, ECR 이미지가 base에 있으므로 test를 아무리 destroy해도 유지된다.

## 실행 순서 체크리스트

1. **base apply**
   ```bash
   cd terraform/base
   terraform init
   terraform apply
   ```
2. **Spaceship에 ACM 검증 레코드 입력**
   ```bash
   terraform output acm_validation_records
   ```
   출력된 CNAME의 `name`에서 도메인 뒷부분을 뺀 호스트 부분을 Spaceship DNS Host에, `value`를 Value에 입력.
3. **인증서 발급 확인 후 443 리스너 생성** (보통 수 분~30분)
   ```bash
   aws acm describe-certificate --certificate-arn $(terraform output -raw acm_certificate_arn) \
     --query 'Certificate.Status'   # "ISSUED" 확인
   terraform apply -var certificate_validated=true
   ```
4. **`api.cluverse.cona.team` CNAME → ALB DNS** (Spaceship)
   ```bash
   terraform output alb_dns_name
   ```
5. **이미지 ECR 푸시**
   ```bash
   aws ecr get-login-password --region ap-northeast-2 | \
     docker login --username AWS --password-stdin $(terraform output -raw ecr_repository_url | cut -d/ -f1)
   docker build -t $(terraform output -raw ecr_repository_url):latest .
   docker push $(terraform output -raw ecr_repository_url):latest
   ```
6. **test apply**
   ```bash
   cd ../test
   terraform init
   terraform apply -var my_ip=$(curl -s ifconfig.me)/32 \
     -var db_password='...8자이상...' \
     -var ssh_public_key="$(cat ~/.ssh/id_ed25519.pub)"
   ```
7. **터널로 Grafana 연결**
   ```bash
   terraform output -raw ssh_tunnel_command   # 실행 후 http://localhost:3000 (admin/admin)
   # 또는 SSH 없이: terraform output ssm_port_forward_examples
   ```
8. 이후에는 **test만 `apply`/`destroy` 반복**. DB/Redis는 EC2 직접 설치라 destroy 시 데이터가 휘발된다(매 테스트 클린 상태 — 의도된 동작).

## 변수 (test)

| 변수 | 필수 | 기본값 | 설명 |
|------|------|--------|------|
| `my_ip` | ✅ | - | Bastion SSH 허용 CIDR (예: `1.2.3.4/32`) |
| `db_password` | ✅ | - | MySQL 비밀번호 (8자 이상, sensitive) |
| `ssh_public_key` | | `""` | 비우면 키페어 없이 생성(SSM 접근만) |
| `db_name` / `db_username` | | `cluverse_v2` / `cluverse_user` | application.yml과 일치 |
| `ecs_desired_count` | | `1` | ECS 태스크 수 |
| `ecs_instance_type` / `db_instance_type` / `redis_instance_type` / `monitoring_instance_type` | | `t3.small`/`t3.small`/`t3.micro`/`t3.small` | 인스턴스 타입 |
| `container_image_tag` | | `latest` | ECR 이미지 태그 |

## 설계 메모

- **NAT를 test에 두는 이유**: 프라이빗 인스턴스가 user_data로 MySQL/Redis/Docker/exporter를 설치하므로 egress가 필요하지만, base에 두면 idle 시간에도 시간당 요금이 나간다. 대안으로 소프트웨어를 미리 구운 커스텀 AMI를 쓰면 NAT 없이 더 빠르고 싸다 — `test/nat.tf` 주석 참고.
- **IAM 3역할 분리**: 컨테이너 인스턴스 역할 / task execution role / task role — `test/iam.tf` 주석 참고.
- **헬스체크**: TG는 `/actuator/health`(matcher 200), ECS 서비스 grace period 180초(Spring Boot 부팅 대비).
- **모니터링**: mysqld_exporter(9104)·redis_exporter(9121)는 고정 IP static scrape, node_exporter(9100)는 `tag:NodeExporter=true` EC2 SD로 자동 발견. 앱 `/actuator/prometheus`는 동적 포트라 제외 — 필요 시 `test/templates/monitoring.sh.tpl` 주석 참고.
- **SG는 계단식 source SG 참조**: ALB→ECS(동적포트), ECS→DB(3306)/Redis(6379), monitoring→exporter 포트, bastion→22/터널 포트. 공인 IP 인바운드는 bastion 22(`my_ip`)와 ALB 80/443뿐.
- 루트의 기존 `main.tf`/`variables.tf`는 구(단일 스택) 구성으로, 이 디렉터리와 무관하다.
