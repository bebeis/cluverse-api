# AWS 측정 사이클 자동화 스크립트

프리티어 크레딧을 아끼기 위해 **측정할 때만 test 스택을 켜는** 운영을 전제로,
`terraform/README.md`의 수동 체크리스트를 스크립트로 묶은 것입니다.

```
최초 1회   base-up.sh     VPC/ALB/ACM/ECR 생성 (Spaceship DNS 입력 2건만 수동)
─────────────────────────────────────────────────────────────
측정 시작  up.sh          이미지 확보 → apply → 앱 healthy 대기 → 시드 → 접속 안내
측정 중    tunnel.sh      Grafana/Prometheus/MySQL/Redis SSH 터널
코드 변경  push-image.sh  bootJar → linux/amd64 빌드 → ECR 푸시 → ECS 재배포
측정 종료  down.sh        test 전체 destroy + 잔존 과금 리소스 점검
```

## 일상 사이클

```bash
script/aws/up.sh            # 딸깍 — 기본으로 view-count 시드까지 적재 (post-list 측정도 커버)
script/aws/tunnel.sh start  # Grafana http://localhost:3000
# … k6 측정 (script/post-list/README.md, script/view-count/README.md) …
script/aws/down.sh          # 딸깍 — 시간당 과금 전부 정지
```

- `up.sh --seed post-list|view-count|full|none`, `--8m`(핫보드 +700만), `--push`(이미지 재빌드)
- 시딩은 **bastion에서 nohup으로** 돌아가므로 로컬이 끊겨도 계속됩니다.
  진행 확인: `seed.sh --follow`, 재적재: `seed.sh view-count --wait`
- 변수 입력은 필요 없습니다. `terraform/test/secrets.auto.tfvars`(gitignore)를 up.sh가 관리합니다
  — `db_password`는 최초 1회 자동 생성 후 유지, `my_ip`는 실행 시마다 현재 공인 IP로 갱신,
  `ssh_public_key`는 리포 루트의 `cluverse-key.pub`.

## 전제 조건

- 로컬 도구: aws CLI, terraform, docker(데몬 실행), k6 — 모두 설치돼 있음
- AWS 자격증명: `aws configure --profile cluverse-terraform` — 스크립트가 이 프로필을 기본으로
  사용한다 (terraform 포함). 다른 프로필을 쓰려면 `AWS_PROFILE=xxx script/aws/up.sh`
- 리포 루트의 `cluverse-key`/`cluverse-key.pub` (bastion SSH·시딩·터널에 사용)

## 비용 메모

- `down.sh` 후 시간당 과금은 **base의 ALB(월 ~$16)** 만 남습니다. 장기간 측정 계획이 없으면
  `terraform -chdir=terraform/base destroy`로 base도 내릴 수 있지만, 다시 올릴 때
  ACM 검증·CNAME(Spaceship DNS 입력)을 다시 해야 합니다 (`base-up.sh` 재실행으로 절차 자동 안내).
- `validated.auto.tfvars`(443 리스너 유지 플래그)와 `secrets.auto.tfvars`는 gitignore 대상이라
  리포를 새로 받으면 사라집니다 — 각각 `base-up.sh` 재실행 / `up.sh` 재실행이면 복구됩니다.

## 여담

- SSH 대신 SSM으로 접근하려면 `terraform -chdir=terraform/test output ssm_port_forward_examples`.
- 앱 healthy 판정은 ALB 타깃그룹 상태로 하며, healthy = Flyway 마이그레이션 완료이므로
  그 직후 시딩해도 안전합니다.
