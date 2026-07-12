-- ===========================================================================
-- v3-count.sql — V3 페이지 블록 상한 카운트의 EXPLAIN
-- ===========================================================================
-- V3 는 V2(deferred join)에 남은 "전체 COUNT" 병목을 없앤다. 총 게시글 수를 정확히
-- 세지 않고, 현재 페이지가 속한 "블록(10페이지 단위)" 까지만 있는지 확인할 만큼만
-- 센다. COUNT 대상 행 수를 search_limit 로 상한하는 게 핵심.
--
-- [상한 공식]
--   search_limit = (((page - 1) / 10) + 1) * size * 10 + 1
--     · (page-1)/10 : 현재 페이지가 속한 10페이지 블록 인덱스(0-base)
--     · +1, *size*10 : 그 블록 끝까지의 행 수
--     · +1          : "다음 블록이 존재하는가(hasNextBlock)" 판정용 여유 1행
--   예) page=1,   size=20 → (0+1)*200+1 = 201
--       page=500, size=20 → (49+1)*200+1 = 10001   (V3 최대 페이지)
--   → 아무리 깊은 페이지라도 COUNT 는 최대 search_limit 행만 훑는다.
--
-- 파라미터:
--   @board_id     : 대상 게시판 (핫보드 2001001)
--   search_limit  : 위 공식 결과. 아래 예시는 page=500,size=20 → 10001 (LIMIT 리터럴)
--
-- ※ MySQL 은 LIMIT 에 사용자 변수를 못 쓰므로 리터럴로 둔다(v1-list.sql 참고).
--   page 를 바꿔 실험하려면 공식으로 계산한 값을 아래 LIMIT 숫자에 직접 넣는다.
-- ---------------------------------------------------------------------------
SET @board_id = 2001001;

-- [확인 포인트]
--   * 파생 테이블(derived) 안쪽이 idx_post_board_status_created_id 커버링(Using index)
--   * 안쪽 rows 가 search_limit(=10001) 로 상한 → v1/v2-count 의 100만+ 와 대비
--   * EXPLAIN ANALYZE 에서 실제 스캔 행이 search_limit 을 넘지 않음을 확인

EXPLAIN
SELECT COUNT(*) AS capped_count
FROM (
    SELECT p.post_id
    FROM post p
    WHERE p.board_id = @board_id
      AND p.status = 'ACTIVE'
    LIMIT 10001
) capped;

EXPLAIN ANALYZE
SELECT COUNT(*) AS capped_count
FROM (
    SELECT p.post_id
    FROM post p
    WHERE p.board_id = @board_id
      AND p.status = 'ACTIVE'
    LIMIT 10001
) capped;
