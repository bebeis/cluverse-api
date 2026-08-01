-- V2: 변경된 게시글 한 건의 판정 스냅샷.
SET @post_id = 9100000001;

EXPLAIN
SELECT p.post_id,
       p.board_id,
       p.status,
       p.created_at,
       COALESCE(plc.like_count, 0) AS like_count,
       COALESCE(pcc.comment_count, 0) AS comment_count,
       COALESCE(pvc.view_count, 0) AS view_count
FROM post p
LEFT JOIN post_like_count plc ON plc.post_id = p.post_id
LEFT JOIN post_comment_count pcc ON pcc.post_id = p.post_id
LEFT JOIN post_view_count pvc ON pvc.post_id = p.post_id
WHERE p.post_id = @post_id;

EXPLAIN ANALYZE
SELECT p.post_id,
       p.board_id,
       p.status,
       p.created_at,
       COALESCE(plc.like_count, 0) AS like_count,
       COALESCE(pcc.comment_count, 0) AS comment_count,
       COALESCE(pvc.view_count, 0) AS view_count
FROM post p
LEFT JOIN post_like_count plc ON plc.post_id = p.post_id
LEFT JOIN post_comment_count pcc ON pcc.post_id = p.post_id
LEFT JOIN post_view_count pvc ON pvc.post_id = p.post_id
WHERE p.post_id = @post_id;

-- 확인: post와 카운트 테이블 모두 PK 단건 접근이며 actual rows가 입력 ID 수에 비례해야 한다.
