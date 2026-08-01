-- V1: 최근 48시간 ACTIVE 게시글 전체를 키셋 청크로 읽는 기준선.
-- @after_post_id를 반복해서 바꾸며 모든 청크의 actual rows 합계를 기록한다.
SET @chunk_size = 1000;
SET @window_start = NOW() - INTERVAL 48 HOUR;
SET @after_created_at = @window_start;
SET @after_post_id = 0;

EXPLAIN
SELECT p.post_id,
       p.board_id,
       p.created_at,
       COALESCE(plc.like_count, 0) AS like_count,
       COALESCE(pcc.comment_count, 0) AS comment_count,
       COALESCE(pvc.view_count, 0) AS view_count
FROM post p
LEFT JOIN post_like_count plc ON plc.post_id = p.post_id
LEFT JOIN post_comment_count pcc ON pcc.post_id = p.post_id
LEFT JOIN post_view_count pvc ON pvc.post_id = p.post_id
WHERE p.status = 'ACTIVE'
  AND p.created_at >= @window_start
  AND (
       p.created_at > @after_created_at
       OR (p.created_at = @after_created_at AND p.post_id > @after_post_id)
  )
ORDER BY p.created_at, p.post_id
LIMIT 1000;

EXPLAIN ANALYZE
SELECT p.post_id,
       p.board_id,
       p.created_at,
       COALESCE(plc.like_count, 0) AS like_count,
       COALESCE(pcc.comment_count, 0) AS comment_count,
       COALESCE(pvc.view_count, 0) AS view_count
FROM post p
LEFT JOIN post_like_count plc ON plc.post_id = p.post_id
LEFT JOIN post_comment_count pcc ON pcc.post_id = p.post_id
LEFT JOIN post_view_count pvc ON pvc.post_id = p.post_id
WHERE p.status = 'ACTIVE'
  AND p.created_at >= @window_start
  AND (
       p.created_at > @after_created_at
       OR (p.created_at = @after_created_at AND p.post_id > @after_post_id)
  )
ORDER BY p.created_at, p.post_id
LIMIT 1000;

-- 다음 청크에서는 마지막 created_at과 post_id를 두 커서 변수에 함께 대입한다.
-- 확인: idx_post_popularity_scan의 range 접근과 한 실행의 actual rows 합계가
-- 최근 48시간 ACTIVE 게시글 수에 비례하는지 확인한다.
