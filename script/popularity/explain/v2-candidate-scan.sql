-- 만기 후보의 제한 배치 claim. EXPLAIN 뒤 실제 잠금 형태는 트랜잭션에서 롤백한다.
SET @batch_size = 500;
SET @now = NOW();

EXPLAIN
SELECT pc.post_id
FROM popularity_candidate pc
WHERE pc.next_check_at <= @now
ORDER BY pc.next_check_at, pc.post_id
LIMIT 500
FOR UPDATE;

START TRANSACTION;
SELECT pc.post_id
FROM popularity_candidate pc
WHERE pc.next_check_at <= @now
ORDER BY pc.next_check_at, pc.post_id
LIMIT 500
FOR UPDATE;
ROLLBACK;

-- 확인: idx(next_check_at, post_id) range 접근, filesort 부재, 반환 행 수가 batch_size 이하인지.
-- 현재 구현은 SKIP LOCKED가 없으므로 다중 워커에서는 락 대기 시간도 함께 기록한다.
