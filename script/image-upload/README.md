# devlog-11 외부 이미지 프로세서 측정

한 번 배포된 애플리케이션의 세 URL을 같은 입력과 같은 외부 processor 조건으로 비교한다.

| URL | 차이 |
|---|---|
| `POST /api/v1/image-uploads` | 이미지별 staging PUT → Lambda 동기 호출을 순차 실행 |
| `POST /api/v2/image-uploads` | `CompletableFuture` + 고정 Platform executor로 이미지별 호출을 겹쳐 실행 |
| `POST /api/v3/image-uploads` | `CompletableFuture` + Virtual Thread executor, Semaphore로 외부 호출 상한 유지 |

DB의 `PENDING → COMPLETED/FAILED`, deterministic object key, requestId 멱등성, 보상과 재조정은 세 버전에 공통이다. 따라서 버전 비교에서 바뀌는 독립 변수는 실행/합류 모델뿐이다.

## 실행

테스트 배포에는 다음 설정이 필요하다.

```text
IMAGE_UPLOAD_EXPERIMENT_ENABLED=true
IMAGE_UPLOAD_BENCHMARK_TOKEN=...
IMAGE_PROCESSOR_LAMBDA_NAME=...
```

실제 카메라 이미지 fixture를 지정한다. 작은 합성 이미지는 리사이즈/전송 비용을 대표하지 못하므로 저장소에 고정 fixture를 넣지 않았다.

```bash
BASE_URL=https://test-api.example.com \
BENCHMARK_TOKEN=... \
IMAGE_FILE=/absolute/path/to/camera.jpg \
IMAGE_COUNT=3 VUS=4 DURATION=30s \
PROMETHEUS_URL=http://localhost:9090 \
./script/image-upload/run.sh
```

각 버전은 warm-up 후 따로 실행된다. `results/<timestamp>/evidence.md`는 캡처용 숫자 표, `latency.svg`는 p95/p99 그래프다.

## 핵심 지표

- 주 비교: 업로드 완료 p95/p99와 처리량. 이미지가 여러 장일 때 순차 합계가 실제로 줄었는지 본다.
- 포화 원인: V2 executor queue 대기와 V3 Semaphore 대기. Lambda/S3 상한을 애플리케이션 개선으로 오인하지 않는다.
- 격리 확인: 같은 부하 동안 `/actuator/health` p95와 JVM platform thread 수.
- 정합성: 실패 주입 뒤 누락 metadata가 있는 `COMPLETED`와 오래된 `PENDING`이 0으로 수렴하는지 본다.

결과 이미지 크기/감소율은 세 버전이 같은 외부 processor 정책을 호출했다는 통제 지표일 뿐, 실행 모델의 개선 지표로 해석하지 않는다.

## 실패와 JFR

```bash
BENCHMARK_TOKEN=... IMAGE_FILE=/absolute/path/to/camera.jpg \
FAILURE_POINT=AFTER_FIRST_OBJECT ./script/image-upload/failure-smoke.sh

mysql ... < script/image-upload/consistency.sql

MYSQL_HOST=127.0.0.1 MYSQL_USER=cluverse_user MYSQL_PASSWORD=... MYSQL_DATABASE=cluverse_v2 \
python3 script/image-upload/verify_consistency.py --bucket cluverse-images \
  --output /tmp/devlog11-consistency.json

JAVA_PID=12345 DURATION=60s OUTPUT=/tmp/devlog11.jfr \
./script/image-upload/capture-jfr.sh
```

`REMOTE_TIMEOUT`은 실행 완료 여부가 불확실하므로 즉시 output을 지우지 않고 PENDING으로 남긴 뒤 stale 기준 이후 재조정한다. JFR은 JDK 21에서 기록하고, 기본 20ms threshold의 `jdk.VirtualThreadPinned`가 0건이면 그대로 0건이라고 남긴다. stack trace를 로그로도 확인하려면 애플리케이션 시작 전에 `JAVA_TOOL_OPTIONS=-Djdk.tracePinnedThreads=full`을 추가한다.

`verify_consistency.py`는 DB의 예정 key와 S3 `image-uploads/` prefix를 대조해 누락된 완료 객체, DB에 대응하지 않는 객체, 삭제 완료로 표시됐지만 남은 staging, stale PENDING 네 숫자만 출력한다. 정상 재조정 이후에는 모두 0이어야 한다.

`EXPLAIN`은 이번 비교에 포함하지 않는다. 병목 가설이 DB 조회 계획이 아니라 외부 동기 I/O의 직렬 합계와 executor 대기이기 때문이다.
