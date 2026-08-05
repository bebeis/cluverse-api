# Devlog 5 조회수 집계 방식 측정

한 번의 배포에서 네 엔드포인트를 같은 데이터와 부하로 비교한다.

| 버전 | 엔드포인트 | 집계 방식 |
|---|---|---|
| V1 | `POST /api/v1/posts/{postId}/view-count` | 요청마다 MySQL 원자적 UPDATE |
| V2 | `POST /api/v2/posts/{postId}/view-count` | Redis 시간 기반 delta + 주기 flush |
| V3 | `POST /api/v3/posts/{postId}/view-count` | Redis threshold delta + 기준 도달 시 flush |
| V4 | `POST /api/v4/posts/{postId}/view-count` | Redis 전체 조회수 + MySQL 체크포인트 |

V2~V4는 `(postId, cluverse_viewer 쿠키)` 중복 방지 조건이 같다. 벤치마크는 매 요청에 새 쿠키를 보내 모든 요청을 유효 조회로 만든다. 중복 방지 자체는 `correctness` 시나리오로 따로 검증한다.

## 핵심 지표

블로그에 가져갈 지표는 다음 세 개로 제한한다.

1. 단일 핫 게시글에서의 달성 RPS와 요청 p99
2. 분산 게시글 부하에서의 달성 RPS와 요청 p99
3. V4 상주 카운터 수·Redis 메모리와 체크포인트 워커 지연

에러율은 성능 수치의 유효성을 확인하는 보조 판정값이다. 평균 지연은 본문 핵심 표에 넣지 않는다.

## 실행

```bash
python3 -m pip install -r script/view-count/requirements.txt

# 먼저 중복 방지와 V4 단조 증가를 확인
script/view-count/run.sh correctness -e VERSION=v4 -e POST_ID=5999999

# 단일 핫 키. 각 버전을 3회 이상 반복한다.
for version in v1 v2 v3 v4; do
  script/view-count/run.sh bench -e VERSION="$version" -e POST_MODE=hot \
    -e POST_ID=5999999 -e RATE=300 -e DURATION=2m
done

# 많은 글로 분산된 입력
for version in v1 v2 v3 v4; do
  script/view-count/run.sh bench -e VERSION="$version" -e POST_MODE=distributed \
    -e POST_ID_MIN=5900000 -e POST_ID_MAX=5999999 -e RATE=300 -e DURATION=2m
done
```

각 실행은 `results/raw/`에 k6 summary JSON과 캡처용 HTML 대시보드를 만든다. `collect_results.py`가 `results/metrics.csv`에 한 행을 추가한다.

AWS 측정 환경은 `script/aws/seed.sh view-count --wait`가 MySQL 시드를 다시 적재하고 조회수
실험용 Redis 키를 초기화한다. 로컬이나 이미 열린 Redis 터널에서 Redis 상태만 초기화할 때는
다음 명령을 사용한다. `FLUSHDB`가 아니라 V2~V4 전용 prefix만 제거한다.

```bash
script/view-count/reset_redis.sh
```

## Redis 상태 캡처

부하 직전과 직후에 같은 명령을 실행한다.

```bash
script/view-count/capture_state.sh
```

출력에는 `used_memory`, V2/V3 delta 키 수, V4 전체 카운터와 중복 방지 키 수만 담긴다. 전후 차이를 사용해야 다른 Redis 데이터의 메모리를 조회수 비용으로 잘못 계산하지 않는다.

V4 워커는 `/actuator/prometheus`에서 아래 두 메트릭만 함께 캡처한다.

- `view_count_worker_duration_seconds{operation="checkpoint"}`
- `view_count_worker_processed{operation="checkpoint"}`
- `view_count_checkpoint_lag_seconds`

배출 속도가 잠시 유입 속도보다 낮은 것은 실패가 아니다. 마지막 성공 체크포인트의 지연이 계속 증가하는지, 피크 뒤 회복하는지가 판정 기준이다.

## EXPLAIN ANALYZE

```bash
mysql -h127.0.0.1 -ucluverse_user -p cluverse_v2 < script/view-count/explain/v1-mysql-increment.sql
mysql -h127.0.0.1 -ucluverse_user -p cluverse_v2 < script/view-count/explain/v2-v3-delta-flush.sql
mysql -h127.0.0.1 -ucluverse_user -p cluverse_v2 < script/view-count/explain/v4-checkpoint.sql
```

모두 PK 단건 UPDATE라 실행 계획 자체보다 호출 빈도와 락 대기가 차이를 만든다. `lock-waits.sql`은 부하 중 별도 세션에서 캡처한다.

## Matplotlib 그래프

```bash
python3 script/view-count/plot_results.py
```

`results/view-count-comparison.png`에 hot 조건의 버전별 달성 RPS와 p99 두 패널만 생성한다. 분산 조건은 다음처럼 별도 이미지로 만든다.

```bash
python3 script/view-count/plot_results.py --workload distributed \
  --output script/view-count/results/view-count-distributed-comparison.png
```

## 공정한 비교 체크리스트

- 같은 인스턴스·JVM·DB·Redis 사양과 같은 시드 사용
- 버전별 워밍업 1회 결과 폐기
- 측정 순서 무작위화, 각 조건 최소 3회 반복
- 실행 사이 V2/V3 delta와 V4 실험 키 정리 또는 전용 postId 범위 분리
- k6 발생률이 아니라 실제 `achieved_rps` 기록
- p99와 함께 에러율·드롭된 iteration 확인
