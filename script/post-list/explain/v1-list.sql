-- ===========================================================================
-- v1-list.sql — V1(naive offset 풀 조인) 목록 쿼리의 EXPLAIN
-- ===========================================================================
-- V1 은 페이지 프로젝션 전체(7조인)를 하나의 쿼리로 뽑으면서 LIMIT/OFFSET 을 건다.
-- offset 이 커지면 "버리기 위해 읽는" 행이 폭증하는 게 이 파일의 관전 포인트다.
--
-- 파라미터:
--   @board_id : 대상 게시판 (핫보드 2001001 권장 — 깊은 offset 재현 가능)
--   size      : 페이지 크기 (기본 20) — LIMIT 리터럴
--   offset    : (page-1) * size.  page=20000,size=20 → 399980 (≈ offset 40만) — OFFSET 리터럴
--
-- ※ MySQL 은 LIMIT/OFFSET 에 사용자 변수를 직접 못 쓴다(문법 오류). WHERE 조건만
--   @board_id 로 변수화하고, LIMIT/OFFSET 은 리터럴로 둔다. offset 을 0/100000/399980
--   으로 바꿔가며 실행하려면 아래 숫자를 직접 편집하라. (변수화가 꼭 필요하면 PREPARE 사용)
-- ---------------------------------------------------------------------------
SET @board_id = 2001001;

-- [확인 포인트]
--   * post 접근이 idx_post_board_status_created_id 를 타는가 (key 컬럼)
--   * rows/filtered: OFFSET 이 커질수록 스캔 후 버려지는 행이 늘어난다.
--     offset 을 0 → 100000 → 399980 으로 바꿔가며 EXPLAIN ANALYZE 의
--     "actual rows" 와 실행시간이 어떻게 뛰는지 비교하는 게 핵심.
--   * post_image 는 display_order=0 썸네일만 LEFT JOIN (다중 이미지 중 대표 1장)
--   * 4개 count 테이블 + member(INNER) + member_profile(LEFT) 은 모두 PK 매칭 조인

-- 7조인 풀 프로젝션 쿼리 (EXPLAIN / EXPLAIN ANALYZE 대상 동일)
EXPLAIN
SELECT
    p.post_id,
    p.title,
    SUBSTRING(p.content, 1, 120) AS content_preview,
    p.category,
    p.is_anonymous,
    p.is_pinned,
    p.created_at,
    p.member_id,
    m.nickname,
    mp.profile_image_url,
    pi.image_url                       AS thumbnail_url,
    COALESCE(pvc.view_count, 0)        AS view_count,
    COALESCE(plc.like_count, 0)        AS like_count,
    COALESCE(pcc.comment_count, 0)     AS comment_count,
    COALESCE(pbc.bookmark_count, 0)    AS bookmark_count
FROM post p
    JOIN      member m                ON m.member_id = p.member_id
    LEFT JOIN member_profile mp       ON mp.member_id = p.member_id
    LEFT JOIN post_image pi           ON pi.post_id = p.post_id AND pi.display_order = 0
    LEFT JOIN post_view_count pvc     ON pvc.post_id = p.post_id
    LEFT JOIN post_like_count plc     ON plc.post_id = p.post_id
    LEFT JOIN post_comment_count pcc  ON pcc.post_id = p.post_id
    LEFT JOIN post_bookmark_count pbc ON pbc.post_id = p.post_id
WHERE p.board_id = @board_id
  AND p.status = 'ACTIVE'
ORDER BY p.created_at DESC, p.post_id DESC
LIMIT 20 OFFSET 399980;

EXPLAIN ANALYZE
SELECT
    p.post_id,
    p.title,
    SUBSTRING(p.content, 1, 120) AS content_preview,
    p.category,
    p.is_anonymous,
    p.is_pinned,
    p.created_at,
    p.member_id,
    m.nickname,
    mp.profile_image_url,
    pi.image_url                       AS thumbnail_url,
    COALESCE(pvc.view_count, 0)        AS view_count,
    COALESCE(plc.like_count, 0)        AS like_count,
    COALESCE(pcc.comment_count, 0)     AS comment_count,
    COALESCE(pbc.bookmark_count, 0)    AS bookmark_count
FROM post p
    JOIN      member m                ON m.member_id = p.member_id
    LEFT JOIN member_profile mp       ON mp.member_id = p.member_id
    LEFT JOIN post_image pi           ON pi.post_id = p.post_id AND pi.display_order = 0
    LEFT JOIN post_view_count pvc     ON pvc.post_id = p.post_id
    LEFT JOIN post_like_count plc     ON plc.post_id = p.post_id
    LEFT JOIN post_comment_count pcc  ON pcc.post_id = p.post_id
    LEFT JOIN post_bookmark_count pbc ON pbc.post_id = p.post_id
WHERE p.board_id = @board_id
  AND p.status = 'ACTIVE'
ORDER BY p.created_at DESC, p.post_id DESC
LIMIT 20 OFFSET 399980;
