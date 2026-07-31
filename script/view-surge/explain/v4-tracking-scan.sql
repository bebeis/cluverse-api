-- ===========================================================================
-- v4-tracking-scan.sql — 라우팅 캐시 갱신/정리 스캔 EXPLAIN
-- ===========================================================================
-- V4 는 요청마다 "이 게시글이 급상승 중인가"를 DB 에 묻지 않는다. 주기적으로
-- (routing-refresh-interval 기본 3초) 활성 급상승 목록을 통째로 읽어 애플리케이션
-- 메모리 캐시를 갈아끼우고, 요청은 그 캐시만 본다. 이 파일은 그 주기 스캔
-- 두 종류의 플랜을 본다.
--   [A] 활성 스캔 — 만료 전 게시글 목록 (라우팅 캐시 갱신용, 3초마다)
--                   LIMIT = routing-cache-max-size (기본 100000)
--   [B] 만료 스캔 — 정리 대상 목록 (cleanup-interval 기본 10초마다)
--                   컷오프 = NOW() - grace(기본 15초), LIMIT = cleanup-batch-size (기본 100)
--
-- 파라미터:
--   @cutoff : 정리 컷오프. 앱은 NOW() - grace 를 자바에서 계산해 넘긴다.
--
-- [확인 포인트]
--   * 두 쿼리 모두 key=idx_expires_at 이어야 한다. 인덱스를 못 타면 3초마다
--     전체 테이블 스캔이 도는 셈이라, 급상승 게시글이 늘수록 스캔 비용이
--     선형으로 커진다. type=range, Extra 에 Using index 가 이상적이다
--     (post_id 만 뽑으므로 커버링 인덱스가 될 수 있는지 확인).
--   * [A] 의 ORDER BY expires_at DESC 는 인덱스 역순 스캔으로 처리돼
--     Using filesort 가 없어야 한다. filesort 가 뜬다면 정렬을 빼거나
--     인덱스 정의를 재검토한다 — 라우팅 캐시는 순서에 의미가 없다.
--   * [B] 는 ORDER BY post_id 라 범위를 좁힌 뒤 정렬이 붙는다. 정리 배치가
--     100건이라 filesort 가 떠도 비용은 작지만, rows 추정치가 cleanup-batch-size 를
--     크게 웃돌면 정리가 밀려 만료된 행이 쌓이고 있다는 신호다.
--   * 급상승 게시글이 몇 건 없는 상태에서 EXPLAIN 을 뜨면 옵티마이저가 풀스캔을
--     고를 수 있다. 인덱스 사용 여부는 부하 중(추적 행이 있는 상태)에 확인한다.
-- ---------------------------------------------------------------------------
SET @cutoff = NOW() - INTERVAL 15 SECOND;

-- [A] 활성 급상승 목록 (라우팅 캐시 갱신)
EXPLAIN
SELECT post_id
FROM view_surge_tracking
WHERE expires_at > NOW()
ORDER BY expires_at DESC
LIMIT 100000;

EXPLAIN FORMAT=TREE
SELECT post_id
FROM view_surge_tracking
WHERE expires_at > NOW()
ORDER BY expires_at DESC
LIMIT 100000;

-- [B] 정리 대상 목록 (최종 flush + Redis 키 삭제 + 추적 행 삭제)
EXPLAIN
SELECT post_id
FROM view_surge_tracking
WHERE expires_at <= @cutoff
ORDER BY post_id
LIMIT 100;

EXPLAIN FORMAT=TREE
SELECT post_id
FROM view_surge_tracking
WHERE expires_at <= @cutoff
ORDER BY post_id
LIMIT 100;

-- 참고: 실제 실행 계획·소요 시간까지 보려면 SELECT 는 EXPLAIN ANALYZE 가 된다.
EXPLAIN ANALYZE
SELECT post_id
FROM view_surge_tracking
WHERE expires_at > NOW()
ORDER BY expires_at DESC
LIMIT 100000;
