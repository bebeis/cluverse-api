// ---------------------------------------------------------------------------
// traffic-profile.js — 게시글 조회 트래픽 프로파일 (수치 관리의 단일 출처)
// ---------------------------------------------------------------------------
//
// 이 파일은 "실제 커뮤니티 사용자가 어떤 게시글을 얼마나 자주 조회하는가"를
// 모델링한다. 조회수 증가는 조회 요청에 따라붙는 쓰기이므로, 이 분포가 곧
// "조회수 쓰기가 어떤 레코드에 얼마나 집중되는가"를 결정한다.
//
// [분포의 의미]
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
//   ※ 인기글(단일 레코드 집중)은 분포로 표현하지 않고 bench 의 POST_MODE=fixed
//     (단일 게시글 반복)로 따로 측정한다. 인기글 전용 부하 설계는 인기글 편에서.
//
// [오버라이드]
//   env POST_SEGMENTS 에 JSON 문자열을 주면 기본값을 대체한다. 예:
//     -e POST_SEGMENTS='[{"from":1,"to":1,"weight":80},{"from":2,"to":100,"weight":20}]'
//   각 세그먼트: { from, to, weight }  (from<=to, weight>0, depth 기준)
//   세그먼트의 to 가 실제 게시글 수를 넘으면 pickPostId 에서 클램프한다.
// ---------------------------------------------------------------------------

// 세그먼트 상단 경계로 쓰는 논리적 최대 깊이. 실제 상한은 pickPostId 범위로 클램프된다.
export const MAX_DEPTH = 1000000;

export const DEFAULT_POST_SEGMENTS = [
    { from: 1, to: 20, weight: 45 },
    { from: 21, to: 200, weight: 30 },
    { from: 201, to: 5000, weight: 15 },
    { from: 5001, to: MAX_DEPTH, weight: 10 },
];

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

const SEGMENTS = resolveSegments();
const TOTAL_WEIGHT = SEGMENTS.reduce((sum, seg) => sum + seg.weight, 0);

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
// pickPostId(minId, maxId)
//   depth 를 분포로 샘플한 뒤 post_id 로 변환한다.
//   시드 게시글은 post_id 가 최신일수록 크므로: postId = maxId - (depth - 1)
//   - depth 가 (maxId - minId + 1) 을 넘으면 minId 로 클램프한다.
//   반환: minId <= postId <= maxId 인 정수
// ---------------------------------------------------------------------------
export function pickPostId(minId, maxId) {
    const range = Math.max(1, Math.floor(maxId) - Math.floor(minId) + 1);
    const depth = pickDepth(range);
    return Math.floor(maxId) - (depth - 1);
}

// 현재 활성 세그먼트/총가중치를 로그·문서 확인용으로 노출한다.
export function describeProfile() {
    return {
        segments: SEGMENTS,
        totalWeight: TOTAL_WEIGHT,
        maxDepth: MAX_DEPTH,
    };
}
