-- 게시판별 정책 계산 표본. 애플리케이션은 이 snapshot으로 percentile을 계산한다.
SET @board_id = 9100000000;
SET @sample_start = NOW() - INTERVAL 7 DAY;
SET @sample_end = NOW();

EXPLAIN
SELECT p.post_id,
       p.board_id,
       p.created_at,
       COALESCE(
           pp.score_at_promotion,
           COALESCE(plc.like_count, 0) * 3
               + COALESCE(pcc.comment_count, 0) * 2
               + COALESCE(pvc.view_count, 0)
       ) AS sample_score,
       COALESCE(plc.like_count, 0) AS like_count,
       COALESCE(pcc.comment_count, 0) AS comment_count,
       COALESCE(pvc.view_count, 0) AS view_count
FROM post p
LEFT JOIN post_like_count plc ON plc.post_id = p.post_id
LEFT JOIN post_comment_count pcc ON pcc.post_id = p.post_id
LEFT JOIN post_view_count pvc ON pvc.post_id = p.post_id
LEFT JOIN popular_post pp
       ON pp.post_id = p.post_id
      AND pp.algorithm_version = 'V2'
WHERE p.board_id = @board_id
  AND p.status = 'ACTIVE'
  AND p.created_at >= @sample_start
  AND p.created_at < @sample_end;

EXPLAIN ANALYZE
SELECT p.post_id,
       p.board_id,
       p.created_at,
       COALESCE(
           pp.score_at_promotion,
           COALESCE(plc.like_count, 0) * 3
               + COALESCE(pcc.comment_count, 0) * 2
               + COALESCE(pvc.view_count, 0)
       ) AS sample_score,
       COALESCE(plc.like_count, 0) AS like_count,
       COALESCE(pcc.comment_count, 0) AS comment_count,
       COALESCE(pvc.view_count, 0) AS view_count
FROM post p
LEFT JOIN post_like_count plc ON plc.post_id = p.post_id
LEFT JOIN post_comment_count pcc ON pcc.post_id = p.post_id
LEFT JOIN post_view_count pvc ON pvc.post_id = p.post_id
LEFT JOIN popular_post pp
       ON pp.post_id = p.post_id
      AND pp.algorithm_version = 'V2'
WHERE p.board_id = @board_id
  AND p.status = 'ACTIVE'
  AND p.created_at >= @sample_start
  AND p.created_at < @sample_end;

-- 확인: 게시판별 표본 행 수, 카운트 테이블 PK 조인 비용, popular_post 조인 비용.
-- 승격 글은 score_at_promotion, 미승격 글은 현재 카운트 가중합을 표본 점수로 사용한다.
-- percentile 정렬 비용은 JVM 메트릭으로 별도 확인한다.
