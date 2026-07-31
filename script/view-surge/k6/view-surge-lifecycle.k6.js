// ---------------------------------------------------------------------------
// view-surge-lifecycle.k6.js — 급상승 게시글의 생애 주기(감지→전환→복귀) 부하
// ---------------------------------------------------------------------------
//
// 대상 API:
//   V4  POST /api/v4/posts/{postId}/view-count  — 급상승 감지 + Redis Write-back
//   (VERSION 을 v3 로 주면 같은 파형을 baseline 에 그대로 먹여 비교할 수 있다)
//
// 목적: bench 가 "정상 상태의 처리량"을 재는 도구라면, 이 스크립트는 상태 전이를 잰다.
//       한 게시글이 평범하다가 → 갑자기 트래픽이 몰려 급상승으로 감지되고 →
//       Redis 경로로 전환됐다가 → 열기가 식어 MySQL 경로로 복귀하기까지,
//       그 전 구간에서 응답 지연이 어떻게 변하는지를 한 번의 실행으로 관찰한다.
//
// [시나리오 2개 — 동시에 돈다]
//   background : 분포(profile|zipf)로 게시글을 흩뿌리는 배경 트래픽. 고정 도착률.
//                급상승 감지가 "배경 대비 튄 것"을 잡는지 보려면 배경이 있어야 한다.
//                지속 시간 = hotspot 전체 + TAIL — hotspot 이 끝난 뒤에도 계속 돌며
//                감쇠·정리(cleanup) 구간의 서버 상태를 관찰한다.
//   hotspot    : 단일 게시글(HOT_POST_ID)에 램프업 → 유지 → 감쇠 → 저트래픽 유지.
//                이 파형이 급상승의 실체다.
//
// [파형]
//   RAMP_UP 동안 START_RATE → RAMP_TARGET 으로 상승
//   SUSTAIN 동안 RAMP_TARGET 유지        ← 이 구간 안에서 감지·전환이 일어나야 한다
//   DECAY   동안 RAMP_TARGET → DECAY_TO 로 하강
//   COOL    동안 DECAY_TO 유지            ← TTL+유예 경과 후 복귀가 일어나야 한다
//   (그 뒤 TAIL 동안은 background 만 남아 정리 이후 상태를 본다)
//
// [계측]
//   시나리오별 http_req_duration 을 sub-metric 으로 노출한다(항상 통과하는 threshold).
//   배경은 평평한데 hotspot 만 출렁이는지, 아니면 hotspot 경합이 배경까지 끌고
//   들어가는지가 이 두 계열의 비교에서 드러난다.
//   hotspot 요청은 hot_duration Trend 에 따로 쌓아 시계열로 본다.
//
//   ※ "감지 시점 / 전환 완료 시점 / 복귀 시점"은 k6 만으로는 확정할 수 없다.
//     k6 p99 시계열(HTML 리포트) + Grafana 메트릭(view_surge_activation_total,
//     view_count_redis_path_total) + view_surge_tracking 타임스탬프,
//     이 3개를 교차 확인해 결정한다. README 의 본 측정 절차 참고.
//
// [실행 예시]
//   # 표준 급상승 (핫보드 최신글)
//   script/view-surge/run.sh lifecycle -e HOT_POST_ID=5999999
//
//   # 역주행 — 오래된 글이 갑자기 뜨는 경우. 버퍼 풀에 없는 레코드라
//   # MySQL 경로의 초기 지연이 더 크고, 그만큼 Redis 전환 이득이 커야 한다.
//   script/view-surge/run.sh lifecycle -e HOT_POST_ID=3000500 -e TAIL=8m
//   (역주행 대상: 기본 시드면 3000001~5000000. --30m(05d) 적재 시엔 14000501 처럼
//    05d 초반부가 1순위 — created_at이 2024년부터 결정적으로 깔린 진짜 오래된 글.
//    05d 구간에서는 n=post_id-14000000 기준 MOD(n,1000)=500 DELETED 글을 피할 것)
//
// [주의] 총 실행 시간은 기본값 기준 RAMP_UP 30s + SUSTAIN 3m + DECAY 1m +
//        COOL 2m + TAIL 3m = 약 9.5분이다.
//
//        기본 TAIL(3m)로는 정리(복귀)까지 못 본다. 서버 기본 설정이
//        tracking-ttl 5m / extension 5m / grace 15s 라서, 만료 시각은 마지막
//        연장(= flush 델타가 sustain-threshold 100 이상인 마지막 주기, 대략 감쇠
//        중반)에서 다시 5분 뒤로 밀린다. 정리는 그로부터 grace 만큼 더 지나야 돈다.
//        복귀 구간까지 k6 시계열에 담으려면 -e TAIL=8m 이상을 주거나, 서버의
//        view-surge.tracking-ttl 을 낮춰서 측정한다. (README Step 3 참고)
// ---------------------------------------------------------------------------

import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { pickPostId, pickPostIdZipf } from './lib/traffic-profile.js';

const VERSION = (__ENV.VERSION || 'v4').toLowerCase();
if (!['v3', 'v4'].includes(VERSION)) {
    throw new Error(`VERSION 은 v3|v4 중 하나여야 합니다 (현재: "${__ENV.VERSION}")`);
}

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 배경 트래픽 설정
const BG_RATE = Number(__ENV.BG_RATE || 50);
const BG_MODE = (__ENV.BG_MODE || 'profile').toLowerCase();
if (!['profile', 'zipf'].includes(BG_MODE)) {
    throw new Error(`BG_MODE 는 profile|zipf 중 하나여야 합니다 (현재: "${__ENV.BG_MODE}")`);
}
const POST_ID_MIN = Number(__ENV.POST_ID_MIN || 5000001);
const POST_ID_MAX = Number(__ENV.POST_ID_MAX || 13999999);

// 급상승 대상 게시글 (역주행 측정이면 3000001~5000000 구간의 오래된 글로 바꾼다)
const HOT_POST_ID = Number(__ENV.HOT_POST_ID || 5999999);

// 급상승 파형
const START_RATE = Number(__ENV.START_RATE || __ENV.DECAY_TO || 5);
const RAMP_TARGET = Number(__ENV.RAMP_TARGET || 300);
const RAMP_UP = __ENV.RAMP_UP || '30s';
const SUSTAIN = __ENV.SUSTAIN || '3m';
const DECAY_TO = Number(__ENV.DECAY_TO || 5);
const DECAY = __ENV.DECAY || '1m';
const COOL = __ENV.COOL || '2m';
// hotspot 종료 후 배경만 남겨 두는 구간 (감쇠·정리 관찰용)
const TAIL = __ENV.TAIL || '3m';

const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 50);
const MAX_VUS = Number(__ENV.MAX_VUS || Math.max(PRE_ALLOCATED_VUS * 2, 100));
// 램프 정점에서 종료되는 경우가 없도록 bench 보다 넉넉히 둔다.
const GRACEFUL_STOP = __ENV.GRACEFUL_STOP || '30s';

// '90s' / '2m' / '1h30m' 같은 k6 기간 표기를 초로 바꾼다 (배경 지속 시간 합산용).
function parseDurationSeconds(text) {
    const matches = String(text).match(/(\d+(?:\.\d+)?)(ms|s|m|h)/g);
    if (!matches) {
        throw new Error(`기간 표기를 해석할 수 없습니다: "${text}" (예: 30s, 2m, 1h)`);
    }
    const unitSeconds = { ms: 0.001, s: 1, m: 60, h: 3600 };
    return matches.reduce((total, token) => {
        const [, value, unit] = token.match(/(\d+(?:\.\d+)?)(ms|s|m|h)/);
        return total + Number(value) * unitSeconds[unit];
    }, 0);
}

const HOTSPOT_SECONDS =
    parseDurationSeconds(RAMP_UP) +
    parseDurationSeconds(SUSTAIN) +
    parseDurationSeconds(DECAY) +
    parseDurationSeconds(COOL);
// 배경은 hotspot 이 끝난 뒤 TAIL 만큼 더 돈다.
const BACKGROUND_DURATION = `${Math.ceil(HOTSPOT_SECONDS + parseDurationSeconds(TAIL))}s`;

export const options = {
    scenarios: {
        background: {
            executor: 'constant-arrival-rate',
            exec: 'backgroundScenario',
            rate: BG_RATE,
            timeUnit: '1s',
            duration: BACKGROUND_DURATION,
            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,
            gracefulStop: GRACEFUL_STOP,
        },
        hotspot: {
            executor: 'ramping-arrival-rate',
            exec: 'hotspotScenario',
            startRate: START_RATE,
            timeUnit: '1s',
            stages: [
                { target: RAMP_TARGET, duration: RAMP_UP },
                { target: RAMP_TARGET, duration: SUSTAIN },
                { target: DECAY_TO, duration: DECAY },
                { target: DECAY_TO, duration: COOL },
            ],
            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,
            gracefulStop: GRACEFUL_STOP,
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
        // 시나리오별 지연을 summary 에 sub-metric 으로 노출하기 위한 표시용 threshold
        // (항상 통과하는 조건. k6 는 scenario 를 시스템 태그로 자동 부착한다)
        'http_req_duration{scenario:hotspot}': ['max>=0'],
        'http_req_duration{scenario:background}': ['max>=0'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    tags: {
        version: VERSION,
    },
};

// 커스텀 메트릭
const hotDuration = new Trend('hot_duration', true); // hotspot 요청 전용
const viewCountSuccessRate = new Rate('view_count_success_rate');

function increaseViewCount(postId, name) {
    const response = http.post(
        `${BASE_URL}/api/${VERSION}/posts/${postId}/view-count`,
        null,
        { tags: { name } },
    );

    const ok = check(response, {
        'status is 200': (res) => res.status === 200,
    });
    viewCountSuccessRate.add(ok);

    if (!ok && response.status !== 500) {
        console.error(
            `[${VERSION}/${name}] postId=${postId} 실패: status=${response.status}, body=${String(response.body).slice(0, 300)}`,
        );
    }
    return response;
}

export function backgroundScenario() {
    const postId =
        BG_MODE === 'zipf'
            ? pickPostIdZipf(POST_ID_MIN, POST_ID_MAX)
            : pickPostId(POST_ID_MIN, POST_ID_MAX);
    increaseViewCount(postId, 'view_surge_background');
}

export function hotspotScenario() {
    const response = increaseViewCount(HOT_POST_ID, 'view_surge_hotspot');
    hotDuration.add(response.timings.duration);
}
