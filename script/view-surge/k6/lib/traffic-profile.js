// ---------------------------------------------------------------------------
// traffic-profile.js — 게시글 조회 트래픽 프로파일 (수치 관리의 단일 출처)
// ---------------------------------------------------------------------------
//
// 이 파일은 "실제 커뮤니티 사용자가 어떤 게시글을 얼마나 자주 조회하는가"를
// 모델링한다. 조회수 증가는 조회 요청에 따라붙는 쓰기이므로, 이 분포가 곧
// "조회수 쓰기가 어떤 레코드에 얼마나 집중되는가"를 결정한다.
//
// 급상승 편(V4)에서는 배경 트래픽을 만드는 데 쓴다. 급상승은 배경 위에 얹히는
// 단일 게시글 스파이크이므로, 배경 분포가 현실적이지 않으면 "감지 임계값을 넘긴 게
// 스파이크 때문인지 배경 때문인지"를 구분할 수 없다.
//
// [세그먼트 분포 — 기본 모드]
//   대형 커뮤니티(디시인사이드/에펨코리아) 분석 기준:
//   - 대부분의 게시글은 작성 직후, 목록 1페이지에 노출된 짧은 시간 동안만 조회된다.
//   - 목록에서 밀려난 글은 조회가 급감하고, 검색·외부 링크로 드물게 다시 조회된다.
//   - 오래된 글(롱테일)은 InnoDB 버퍼 풀에 없을 가능성이 높아, 조회수 UPDATE 에
//     디스크 I/O 비용이 얹힌다. 이 구간을 남겨 두는 이유다.
//   즉 축은 "최신글 기준 깊이(depth)" — depth 1 = 가장 최근 글.
//
// [기본 세그먼트 가중치]  (세그먼트 내부는 균등 분포)
//   depth 1~20       : 45%  ← 목록 1페이지에 노출 중인 최신글 (조회 집중 구간)
//   depth 21~200     : 30%  ← 최근 며칠 내 글 + 개념글 후보
//   depth 201~5000   : 15%  ← 검색/북마크/외부 링크 유입
//   depth 5001~MAX   : 10%  ← 롱테일 (버퍼 풀 미스 → 디스크 I/O 유발 구간)
//
// [Zipf 분포 — 대안 모드]
//   세그먼트 분포는 구간 경계에서 확률이 계단처럼 꺾인다. 급상승 감지는
//   "배경 대비 얼마나 튀는가"를 보는 기능이라, 경계 계단이 감지 임계값 근처에
//   인공적인 봉우리를 만들 수 있다. 그래서 경계 없이 매끄럽게 감쇠하는
//   Zipf(멱법칙) 분포를 pickPostIdZipf 로 함께 제공한다. 어느 쪽을 쓰든
//   "최신글 집중 + 롱테일"이라는 모양은 같고, 배경의 매끄러움만 달라진다.
//
//   ※ 인기글(단일 레코드 집중)은 분포로 표현하지 않는다. 급상승 대상 게시글은
//     bench 의 POST_MODE=fixed / lifecycle 의 hotspot 시나리오가 따로 때린다.
//     배경 분포에 핫키를 섞으면 "배경이 만든 급상승"과 "우리가 만든 급상승"이
//     섞여 감지 시점 해석이 불가능해진다.
//
// [오버라이드]
//   env POST_SEGMENTS 에 JSON 문자열을 주면 기본값을 대체한다. 예:
//     -e POST_SEGMENTS='[{"from":1,"to":1,"weight":80},{"from":2,"to":100,"weight":20}]'
//   각 세그먼트: { from, to, weight }  (from<=to, weight>0, depth 기준)
//   세그먼트의 to 가 실제 게시글 수를 넘으면 pickPostId 에서 클램프한다.
//   env ZIPF_S 는 Zipf 지수(기본 1.1). 클수록 최신글 쏠림이 강해진다.
// ---------------------------------------------------------------------------

// 세그먼트 상단 경계로 쓰는 논리적 최대 깊이. 실제 상한은 pickPostId 범위로 클램프된다.
export const MAX_DEPTH = 1000000;

export const DEFAULT_POST_SEGMENTS = [
    { from: 1, to: 20, weight: 45 },
    { from: 21, to: 200, weight: 30 },
    { from: 201, to: 5000, weight: 15 },
    { from: 5001, to: MAX_DEPTH, weight: 10 },
];

// Zipf 지수. 1.0 근처가 전형적인 웹 접근 분포, 클수록 상위 소수에 더 쏠린다.
export const DEFAULT_ZIPF_S = 1.1;

// env 오버라이드가 있으면 파싱해서 쓰고, 없거나 잘못됐으면 기본값으로 폴백한다.
function resolveSegments() {
    const raw = __ENV.POST_SEGMENTS;
    if (!raw) {
        return DEFAULT_POST_SEGMENTS;
    }
    try {
        const parsed = JSON.parse(raw);
        if (!Array.isArray(parsed) || parsed.length === 0) {
            throw new Error('POST_SEGMENTS must be a non-empty array');
        }
        for (const seg of parsed) {
            if (
                typeof seg.from !== 'number' ||
                typeof seg.to !== 'number' ||
                typeof seg.weight !== 'number' ||
                seg.from < 1 ||
                seg.to < seg.from ||
                seg.weight <= 0
            ) {
                throw new Error(`invalid segment: ${JSON.stringify(seg)}`);
            }
        }
        return parsed;
    } catch (err) {
        console.error(`[traffic-profile] POST_SEGMENTS 파싱 실패, 기본값 사용: ${err}`);
        return DEFAULT_POST_SEGMENTS;
    }
}

// Zipf 지수도 같은 방식으로 검증 후 폴백한다. s <= 0 은 감쇠가 아니라 증가라 의미가 없다.
function resolveZipfExponent() {
    const raw = __ENV.ZIPF_S;
    if (!raw) {
        return DEFAULT_ZIPF_S;
    }
    const parsed = Number(raw);
    if (!Number.isFinite(parsed) || parsed <= 0) {
        console.error(`[traffic-profile] ZIPF_S 가 잘못됨("${raw}"), 기본값 ${DEFAULT_ZIPF_S} 사용`);
        return DEFAULT_ZIPF_S;
    }
    return parsed;
}

const SEGMENTS = resolveSegments();
const TOTAL_WEIGHT = SEGMENTS.reduce((sum, seg) => sum + seg.weight, 0);
const ZIPF_S = resolveZipfExponent();

// [lo, hi] 범위의 정수 균등 샘플
function uniformInt(lo, hi) {
    return Math.floor(Math.random() * (hi - lo + 1)) + lo;
}

// 가중치에 따라 세그먼트를 하나 고르고, 그 세그먼트 내부에서 균등하게 depth 를 뽑는다.
function pickDepth(maxDepth) {
    const cap = Math.max(1, Math.floor(maxDepth));

    let r = Math.random() * TOTAL_WEIGHT;
    let chosen = SEGMENTS[SEGMENTS.length - 1];
    for (const seg of SEGMENTS) {
        if (r < seg.weight) {
            chosen = seg;
            break;
        }
        r -= seg.weight;
    }

    const lo = Math.min(chosen.from, cap);
    const hi = Math.min(chosen.to, cap);
    return uniformInt(lo, hi);
}

// ---------------------------------------------------------------------------
// pickZipfRank(n)
//   1..n 범위에서 Zipf(멱법칙) 분포를 따르는 순위를 하나 뽑는다.
//
//   [구현: 연속 근사 역CDF]
//   이산 Zipf 의 정확한 CDF 는 조화수 H(k,s)=Σ i^-s 의 누적표가 필요한데,
//   n 이 3천만 규모라 사전 계산이 불가능하다(메모리·기동시간 모두). 그래서
//   밀도 f(x) ∝ x^-s 를 [1, n] 구간의 연속분포로 보고 역CDF 를 닫힌 식으로 푼다.
//       F(x) = (x^(1-s) - 1) / (n^(1-s) - 1)
//       x    = ( u·(n^(1-s) - 1) + 1 )^(1/(1-s))       (s != 1)
//       x    = n^u                                      (s == 1, 위 식의 극한)
//   거절 샘플링이 없어 호출당 상수 시간이고, 난수 1개만 쓴다.
//
//   [정확도 주의]
//   연속 근사이므로 이산 Zipf 와 완전히 같지 않다. 오차는 순위가 작은 쪽
//   (특히 rank 1~3)에 몰리며, s 가 1에서 멀수록 커진다. 실측 목적상
//   "상위 소수에 강하게 쏠리는 매끄러운 롱테일"이면 충분하므로 이 근사를 쓴다.
//   Zipf 계수 자체를 논증하는 용도(예: 실제 로그와의 적합도 비교)로는 부적합하다.
// ---------------------------------------------------------------------------
export function pickZipfRank(n) {
    const cap = Math.max(1, Math.floor(n));
    if (cap === 1) {
        return 1;
    }

    const u = Math.random();
    let rank;
    if (Math.abs(ZIPF_S - 1) < 1e-9) {
        rank = Math.pow(cap, u);
    } else {
        const exp = 1 - ZIPF_S;
        rank = Math.pow(u * (Math.pow(cap, exp) - 1) + 1, 1 / exp);
    }

    // 부동소수 오차로 경계를 살짝 벗어날 수 있어 클램프한다.
    return Math.min(cap, Math.max(1, Math.floor(rank)));
}

// ---------------------------------------------------------------------------
// pickPostId(minId, maxId)
//   depth 를 세그먼트 분포로 샘플한 뒤 post_id 로 변환한다.
//   시드 게시글은 post_id 가 최신일수록 크므로: postId = maxId - (depth - 1)
//   - depth 가 (maxId - minId + 1) 을 넘으면 minId 로 클램프한다.
//   반환: minId <= postId <= maxId 인 정수
// ---------------------------------------------------------------------------
export function pickPostId(minId, maxId) {
    const range = Math.max(1, Math.floor(maxId) - Math.floor(minId) + 1);
    const depth = pickDepth(range);
    return Math.floor(maxId) - (depth - 1);
}

// ---------------------------------------------------------------------------
// pickPostIdZipf(minId, maxId)
//   pickPostId 와 같은 depth→postId 매핑을 쓰되, depth 를 Zipf 로 뽑는다.
//   세그먼트 경계의 계단 없이 최신글에서 롱테일까지 매끄럽게 감쇠한다.
// ---------------------------------------------------------------------------
export function pickPostIdZipf(minId, maxId) {
    const range = Math.max(1, Math.floor(maxId) - Math.floor(minId) + 1);
    const depth = pickZipfRank(range);
    return Math.floor(maxId) - (depth - 1);
}

// 현재 활성 세그먼트/총가중치/Zipf 지수를 로그·문서 확인용으로 노출한다.
export function describeProfile() {
    return {
        segments: SEGMENTS,
        totalWeight: TOTAL_WEIGHT,
        maxDepth: MAX_DEPTH,
        zipfExponent: ZIPF_S,
    };
}
