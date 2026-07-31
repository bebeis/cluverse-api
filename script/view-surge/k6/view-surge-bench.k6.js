// ---------------------------------------------------------------------------
// view-surge-bench.k6.js — 게시글 조회수 증가 V3/V4 공용 벤치마크
// ---------------------------------------------------------------------------
//
// 대상 API:
//   V3  POST /api/v3/posts/{postId}/view-count  — 원자적 UPDATE (기존 baseline)
//   V4  POST /api/v4/posts/{postId}/view-count  — 급상승 감지 + Redis Write-back
//
// 목적 두 가지:
//   1) V3 의 한계선을 찾는다. 단일 게시글에 계단식으로 부하를 올려가며
//      "초당 몇 건부터 MySQL 원자적 UPDATE 가 무너지는가"를 실측한다.
//      이 수치가 곧 V4 급상승 감지 임계값(VIEW_SURGE_THRESHOLD)의 근거가 된다.
//   2) V4 의 상시 오버헤드를 잰다. 급상승이 아닌 평상시 트래픽에서 V4 는
//      V3 와 같은 경로(MySQL 원자적 UPDATE) + 속도 집계만 더한 것이므로,
//      균등 분포 부하에서 두 버전의 차이가 곧 감지 로직의 비용이다.
//
// 게시글 선택 모드 3종:
//   POST_MODE=fixed (기본) + POST_ID=n — 단일 게시글에 쓰기 집중.
//        계단 부하(EXECUTOR=steps)와 짝지어 급상승 임계값을 도출하는 핵심 시나리오.
//   POST_MODE=profile — 세그먼트 분포로 샘플 (최신글 집중 + 롱테일).
//   POST_MODE=zipf    — Zipf 분포로 샘플 (경계 계단이 없는 매끄러운 롱테일).
//   ※ 어느 분포 모드도 핫키를 섞지 않는다. 급상승은 fixed 모드가 따로 만든다.
//
// 실행기 2종:
//   EXECUTOR=constant (기본) — constant-arrival-rate. RATE/DURATION 고정.
//   EXECUTOR=steps           — ramping-arrival-rate. STEP_RATES 를 STEP_DURATION 씩
//        평평하게 밟고 올라간다(계단마다 즉시 점프 후 유지). 각 요청에는 경과
//        시간으로 계산한 현재 계단 rate 가 step 태그로 붙어, 계단별 지연 분포가
//        summary 에 sub-metric 으로 따로 나온다.
//
// [실행 예시]
//   # 1) V3 계단 부하 — 단일 게시글, 어디서 무너지는지 (급상승 임계값 근거)
//   script/view-surge/run.sh bench -e VERSION=v3 -e EXECUTOR=steps \
//          -e STEP_RATES=50,100,150,200,250,300 -e STEP_DURATION=1m
//
//   # 2) V3 vs V4 상시 오버헤드 — 균등 분포에서 같은 조건으로
//   for v in v3 v4; do \
//     script/view-surge/run.sh bench -e VERSION=$v -e POST_MODE=zipf \
//            -e RATE=300 -e DURATION=2m; done
//
// [주의] 기본 POST_ID=5999999 (05a 핫보드 2001001의 ACTIVE 핫 레코드.
//        최신 글 6000000은 시드 규칙상 DELETED).
//        분포 모드 기본 범위는 5000001~13999999 (05a+05b). 05d(일반 게시판 확장)까지
//        넣었다면 -e POST_ID_MAX=30000000 으로 넓힌다.
// ---------------------------------------------------------------------------

import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Rate, Trend } from 'k6/metrics';
import { pickPostId, pickPostIdZipf } from './lib/traffic-profile.js';

const VERSION = (__ENV.VERSION || '').toLowerCase();
if (!['v3', 'v4'].includes(VERSION)) {
    throw new Error(`VERSION 은 v3|v4 중 하나여야 합니다 (현재: "${__ENV.VERSION}")`);
}

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const POST_MODE = (__ENV.POST_MODE || 'fixed').toLowerCase();
if (!['fixed', 'profile', 'zipf'].includes(POST_MODE)) {
    throw new Error(`POST_MODE 는 fixed|profile|zipf 중 하나여야 합니다 (현재: "${__ENV.POST_MODE}")`);
}

const EXECUTOR = (__ENV.EXECUTOR || 'constant').toLowerCase();
if (!['constant', 'steps'].includes(EXECUTOR)) {
    throw new Error(`EXECUTOR 는 constant|steps 중 하나여야 합니다 (현재: "${__ENV.EXECUTOR}")`);
}

// fixed 모드 대상 게시글 (기본 = 05a 핫보드의 ACTIVE 최신 글)
const POST_ID = Number(__ENV.POST_ID || 5999999);
// profile/zipf 모드 게시글 범위 (기본 = 05a+05b: 5000001~13999999)
const POST_ID_MIN = Number(__ENV.POST_ID_MIN || 5000001);
const POST_ID_MAX = Number(__ENV.POST_ID_MAX || 13999999);

const RATE = Number(__ENV.RATE || 100);
const DURATION = __ENV.DURATION || '1m';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 50);
const MAX_VUS = Number(__ENV.MAX_VUS || Math.max(PRE_ALLOCATED_VUS * 2, 100));
// 종료 시 이미 시작된 요청이 끝날 때까지의 대기 상한. 단건 요청이라 짧게 둔다.
const GRACEFUL_STOP = __ENV.GRACEFUL_STOP || '5s';

// steps 모드 계단 정의
const STEP_DURATION = __ENV.STEP_DURATION || '1m';
const STEP_RATES = (__ENV.STEP_RATES || '50,100,150,200,250,300')
    .split(',')
    .map((token) => Number(token.trim()))
    .filter((value) => Number.isFinite(value) && value > 0);
if (EXECUTOR === 'steps' && STEP_RATES.length === 0) {
    throw new Error(`STEP_RATES 는 양수 rps 목록이어야 합니다 (현재: "${__ENV.STEP_RATES}")`);
}

// ---------------------------------------------------------------------------
// 계단 부하는 ramping-arrival-rate 로 만든다. 목표는 "선형 램프"가 아니라
// "평평한 계단"이므로, 계단마다 duration 0 스테이지로 즉시 점프한 뒤 유지한다.
// ---------------------------------------------------------------------------
function buildStepStages() {
    const stages = [];
    for (const stepRate of STEP_RATES) {
        stages.push({ target: stepRate, duration: '0s' });
        stages.push({ target: stepRate, duration: STEP_DURATION });
    }
    return stages;
}

function buildScenario() {
    if (EXECUTOR === 'steps') {
        return {
            executor: 'ramping-arrival-rate',
            exec: 'benchScenario',
            startRate: STEP_RATES[0],
            timeUnit: '1s',
            stages: buildStepStages(),
            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,
            gracefulStop: GRACEFUL_STOP,
        };
    }
    return {
        executor: 'constant-arrival-rate',
        exec: 'benchScenario',
        rate: RATE,
        timeUnit: '1s',
        duration: DURATION,
        preAllocatedVUs: PRE_ALLOCATED_VUS,
        maxVUs: MAX_VUS,
        gracefulStop: GRACEFUL_STOP,
    };
}

// 계단별 지연을 summary 에 노출하기 위한 표시용 threshold.
// (태그별 Trend 는 threshold 로 등록해야 sub-metric 으로 출력된다 — 항상 통과하는 조건)
function buildStepThresholds() {
    if (EXECUTOR !== 'steps') {
        return {};
    }
    const thresholds = {};
    for (const stepRate of STEP_RATES) {
        thresholds[`view_count_duration{step:r${stepRate}}`] = ['max>=0'];
    }
    return thresholds;
}

export const options = {
    scenarios: {
        view_surge_bench: buildScenario(),
    },
    // 측정이 목적이므로 threshold 는 관대하게(실패 처리보다 기록 위주). 에러율만 가드.
    // 계단 부하에서 이 가드에 걸리는 지점이 곧 "V3 가 무너지는 rate" 라는 결과다.
    thresholds: Object.assign(
        {
            http_req_failed: ['rate<0.05'],
        },
        buildStepThresholds(),
    ),
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    tags: {
        version: VERSION,
        post_mode: POST_MODE,
    },
};

// 커스텀 메트릭
const viewCountDuration = new Trend('view_count_duration', true);
const viewCountSuccessRate = new Rate('view_count_success_rate');

const STEP_DURATION_MS = parseDurationMs(STEP_DURATION);

// '90s' / '2m' / '1h30m' 같은 k6 기간 표기를 ms 로 바꾼다 (계단 인덱스 계산용).
function parseDurationMs(text) {
    const matches = String(text).match(/(\d+(?:\.\d+)?)(ms|s|m|h)/g);
    if (!matches) {
        throw new Error(`기간 표기를 해석할 수 없습니다: "${text}" (예: 30s, 2m, 1h)`);
    }
    const unitMs = { ms: 1, s: 1000, m: 60000, h: 3600000 };
    return matches.reduce((total, token) => {
        const [, value, unit] = token.match(/(\d+(?:\.\d+)?)(ms|s|m|h)/);
        return total + Number(value) * unitMs[unit];
    }, 0);
}

// 테스트 시작 후 경과 시간으로 현재 계단을 판정한다.
// (k6 는 스테이지 인덱스를 스크립트에 노출하지 않으므로 시간으로 역산한다.
//  램프 구간이 없는 평평한 계단이라 경계 오차는 계단 전환 순간의 몇 ms 뿐이다.)
function currentStepRate() {
    const elapsed = exec.instance.currentTestRunDuration;
    const index = Math.min(STEP_RATES.length - 1, Math.floor(elapsed / STEP_DURATION_MS));
    return STEP_RATES[Math.max(0, index)];
}

function nextPostId() {
    if (POST_MODE === 'fixed') {
        return POST_ID;
    }
    if (POST_MODE === 'zipf') {
        return pickPostIdZipf(POST_ID_MIN, POST_ID_MAX);
    }
    return pickPostId(POST_ID_MIN, POST_ID_MAX);
}

export function benchScenario() {
    const postId = nextPostId();
    const tags = { name: 'view_surge_bench' };
    if (EXECUTOR === 'steps') {
        tags.step = `r${currentStepRate()}`;
    }

    const response = http.post(
        `${BASE_URL}/api/${VERSION}/posts/${postId}/view-count`,
        null,
        { tags },
    );

    viewCountDuration.add(response.timings.duration, EXECUTOR === 'steps' ? { step: tags.step } : {});

    const ok = check(response, {
        'status is 200': (res) => res.status === 200,
    });

    viewCountSuccessRate.add(ok);

    if (!ok && response.status !== 500) {
        console.error(
            `[${VERSION}] postId=${postId} 실패: status=${response.status}, body=${String(response.body).slice(0, 300)}`,
        );
    }
}
