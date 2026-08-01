-- 만기 후보의 제한 배치 claim. EXPLAIN 뒤 실제 잠금 형태는 트랜잭션에서 롤백한다.
SET @batch_size = 500;
SET @now = NOW();

EXPLAIN
SELECT pc.post_id
FROM popularity_candidate pc
WHERE pc.next_check_at <= @now
ORDER BY pc.next_check_at, pc.post_id
LIMIT 500
FOR UPDATE SKIP LOCKED;

START TRANSACTION;
SELECT pc.post_id
FROM popularity_candidate pc
WHERE pc.next_check_at <= @now
ORDER BY pc.next_check_at, pc.post_id
LIMIT 500
FOR UPDATE SKIP LOCKED;
ROLLBACK;

-- 확인: idx(next_check_at, post_id) range 접근, filesort 부재, 반환 행 수가 batch_size 이하인지.
-- 다중 워커에서는 잠긴 행을 기다리지 않고 건너뛰는지와 워커 수별 처리량을 함께 기록한다.
