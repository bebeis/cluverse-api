-- ===========================================================================
-- lock-waits.sql — 부하 중 락 경합 관찰 스냅샷 모음
-- ===========================================================================
-- k6 부하가 돌아가는 동안 별도 세션에서 실행해 락 경합의 실체를 캡처한다.
-- EXPLAIN 으로는 보이지 않는 "락 보유 시간의 차이"가 여기서 드러난다.
--
-- 사용법:
--   1) 부하 시작 전: 마지막 절(누적 카운터)을 실행해 기준값 기록
--   2) 부하 중: 1~3절을 여러 번 실행해 대기 체인 스냅샷 캡처 (스크린샷 대상)
--   3) 부하 종료 후: 누적 카운터를 다시 실행해 델타 계산 → results 표에 기록
--
-- [확인 포인트 — 버전별 예상 관찰]
--   * V1: 요청마다 같은 PK를 UPDATE하므로 핫 게시글에서 행 락 대기가 집중된다.
--   * V2/V3: 요청 경로는 Redis를 사용하고, flush 시점에만 짧은 UPDATE가 관찰된다.
--   * V4: checkpoint 시점에만 GREATEST 기반 UPDATE가 관찰된다.
-- ---------------------------------------------------------------------------

-- [1] 지금 이 순간의 락 대기 체인 (누가 누구를 기다리는가)
SELECT *
FROM sys.innodb_lock_waits;

-- [2] 원시 락 대기 (요청 트랜잭션 / 보유 트랜잭션 쌍)
SELECT
    REQUESTING_ENGINE_TRANSACTION_ID AS waiting_trx,
    BLOCKING_ENGINE_TRANSACTION_ID   AS blocking_trx
FROM performance_schema.data_lock_waits;

-- [3] 조회수 테이블에 걸려 있는 락 목록
SELECT
    ENGINE_TRANSACTION_ID,
    OBJECT_NAME,
    LOCK_TYPE,
    LOCK_MODE,
    LOCK_STATUS,
    LOCK_DATA
FROM performance_schema.data_locks
WHERE OBJECT_NAME = 'post_view_count';

-- [4] 누적 카운터 (부하 전/후로 실행해 델타를 계산한다)
--   Innodb_row_lock_waits       : 락 대기가 발생한 횟수
--   Innodb_row_lock_time        : 락 대기에 쓴 총 시간(ms)
--   Innodb_row_lock_time_avg/max: 평균/최대 대기 시간(ms)
SHOW GLOBAL STATUS LIKE 'Innodb_row_lock%';
