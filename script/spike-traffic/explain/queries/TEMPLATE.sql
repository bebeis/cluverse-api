-- 느린 API의 실제 SQL과 대표 파라미터로 복사해서 사용한다.
-- 실행 중 데이터를 바꾸는 쿼리는 capture.sh가 거부한다.

SET @viewer_id = 1;

EXPLAIN ANALYZE
SELECT 1;
