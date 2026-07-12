// ---------------------------------------------------------------------------
// traffic-profile.js — 게시글 목록 트래픽 프로파일 (수치 관리의 단일 출처)
// ---------------------------------------------------------------------------
//
// 이 파일은 "실제 게시판 사용자가 어느 페이지(=offset)를 얼마나 자주 보는가"를
// 모델링한다. bench(V1~V3)와 cursor(V4) 스크립트가 모두 이 분포를 재사용해야
// offset 방식과 커서 방식을 "같은 트래픽 패턴"으로 공정하게 비교할 수 있다.
//
// [분포의 의미]
//   실제 커뮤니티 게시판은 조회가 앞페이지에 극단적으로 편중된다.
//   대부분의 사용자는 1페이지(최신글)만 보고 이탈하며, 뒤로 갈수록 급감한다.
//   반대로 "깊은 페이지"는 드물지만 존재하며(북마크·검색 유입·과거글 탐색),
//   바로 이 소수 요청이 naive offset 페이지네이션의 꼬리 지연(p99)을 만든다.
//   따라서 앞페이지에 무게를 실으면서도 깊은 페이지 세그먼트를 남겨 둔다.
//
// [기본 세그먼트 가중치]  (세그먼트 내부는 균등 분포)
//   page 1        : 50%   ← 최신글만 보고 나가는 다수
//   page 2~10     : 30%   ← 한두 번 넘겨보는 사용자
//   page 11~100   : 15%   ← 특정 주제/과거글을 뒤지는 사용자
//   page 101~MAX  :  5%   ← 깊은 탐색(꼬리 지연의 원인)
//
// [오버라이드]
//   env PAGE_SEGMENTS 에 JSON 문자열을 주면 기본값을 대체한다. 예:
//     -e PAGE_SEGMENTS='[{"from":1,"to":1,"weight":70},{"from":2,"to":50,"weight":30}]'
//   각 세그먼트: { from, to, weight }  (from<=to, weight>0)
//   세그먼트의 to 가 실제 maxPage 를 넘으면 pickPage 에서 maxPage 로 클램프한다.
// ---------------------------------------------------------------------------

// 세그먼트 상단 경계로 쓰는 논리적 최대 페이지. 실제 상한은 pickPage(maxPage) 인자로 클램프된다.
export const MAX_PAGE = 20000;

export const DEFAULT_PAGE_SEGMENTS = [
    { from: 1, to: 1, weight: 50 },
    { from: 2, to: 10, weight: 30 },
    { from: 11, to: 100, weight: 15 },
    { from: 101, to: MAX_PAGE, weight: 5 },
];

// env 오버라이드가 있으면 파싱해서 쓰고, 없거나 잘못됐으면 기본값으로 폴백한다.
function resolveSegments() {
    const raw = __ENV.PAGE_SEGMENTS;
    if (!raw) {
        return DEFAULT_PAGE_SEGMENTS;
    }
    try {
        const parsed = JSON.parse(raw);
        if (!Array.isArray(parsed) || parsed.length === 0) {
            throw new Error('PAGE_SEGMENTS must be a non-empty array');
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
        console.error(`[traffic-profile] PAGE_SEGMENTS 파싱 실패, 기본값 사용: ${err}`);
        return DEFAULT_PAGE_SEGMENTS;
    }
}

const SEGMENTS = resolveSegments();
const TOTAL_WEIGHT = SEGMENTS.reduce((sum, seg) => sum + seg.weight, 0);

// [1, hi] 범위의 정수 균등 샘플
function uniformInt(lo, hi) {
    return Math.floor(Math.random() * (hi - lo + 1)) + lo;
}

// ---------------------------------------------------------------------------
// pickPage(maxPage)
//   가중치에 따라 세그먼트를 하나 고르고, 그 세그먼트 내부에서 균등하게 페이지를 뽑는다.
//   - 세그먼트의 to 가 maxPage 를 초과하면 maxPage 로 클램프한다.
//   - 세그먼트 from 이 maxPage 를 초과하면(예: MAX_PAGE=500 인 V3에서 101~20000 세그먼트)
//     해당 세그먼트는 [from..maxPage] 로 좁혀지며, from>maxPage 면 maxPage 단일값이 된다.
//   반환: 1 <= page <= maxPage 인 정수
// ---------------------------------------------------------------------------
export function pickPage(maxPage) {
    const cap = Math.max(1, Math.floor(maxPage));

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

// 현재 활성 세그먼트/총가중치를 로그·문서 확인용으로 노출한다.
export function describeProfile() {
    return {
        segments: SEGMENTS,
        totalWeight: TOTAL_WEIGHT,
        maxPage: MAX_PAGE,
    };
}
