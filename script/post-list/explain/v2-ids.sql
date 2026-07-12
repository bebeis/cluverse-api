-- ===========================================================================
-- v2-ids.sql — V2 커버링 인덱스 deferred join: 1단계 ID 슬라이스
-- ===========================================================================
-- V2 는 "무거운 프로젝션에 OFFSET 을 걸지 않는다". 대신 커버링 인덱스만으로
-- post_id 만 먼저 LIMIT/OFFSET 으로 뽑고(이 파일), 그 소수 id 로만 프로젝션한다
-- (v2-projection.sql). offset 페널티를 인덱스 스캔에 한정시키는 게 핵심.
--
-- 파라미터:
--   @board_id : 대상 게시판 (핫보드 2001001)
--   size+1    : hasNext 판정 위해 size+1 을 LIMIT (size=20 → 21) — LIMIT 리터럴
--   offset    : (page-1) * size.  page=20000,size=20 → 399980 — OFFSET 리터럴
--
-- ※ MySQL 은 LIMIT/OFFSET 에 사용자 변수를 못 쓰므로 리터럴로 둔다(v1-list.sql 참고).
-- ---------------------------------------------------------------------------
SET @board_id = 2001001;

-- [확인 포인트]
--   * Extra 에 "Using index" 가 떠야 한다 = 커버링 인덱스만으로 처리(테이블 접근 0).
--     idx_post_board_status_created_id 가 (board_id, status, created_at, post_id)
--     를 모두 담고 있어 정렬·필터·프로젝션이 인덱스 안에서 끝난다.
--   * key = idx_post_board_status_created_id, type = ref/range
--   * offset 이 커도 "인덱스 엔트리"만 건너뛰므로 v1-list 의 풀행 스캔보다 훨씬 싸다.

EXPLAIN
SELECT p.post_id
FROM post p
WHERE p.board_id = @board_id
  AND p.status = 'ACTIVE'
ORDER BY p.created_at DESC, p.post_id DESC
LIMIT 21 OFFSET 399980;

EXPLAIN ANALYZE
SELECT p.post_id
FROM post p
WHERE p.board_id = @board_id
  AND p.status = 'ACTIVE'
ORDER BY p.created_at DESC, p.post_id DESC
LIMIT 21 OFFSET 399980;
