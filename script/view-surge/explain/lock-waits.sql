-- ===========================================================================
-- lock-waits.sql — 부하 중 락 경합 관찰 스냅샷 모음
-- ===========================================================================
-- k6 부하가 돌아가는 동안 별도 세션에서 실행해 락 경합의 실체를 캡처한다.
-- EXPLAIN 으로는 보이지 않는 "같은 레코드에 쓰기가 몰릴 때의 대기"가 여기서 드러난다.
--
-- 사용법:
--   1) 부하 시작 전: 마지막 절(누적 카운터)을 실행해 기준값 기록
--   2) 부하 중: 1~3절을 여러 번 실행해 대기 체인 스냅샷 캡처 (스크린샷 대상)
--   3) 부하 종료 후: 누적 카운터를 다시 실행해 델타 계산 → results 표에 기록
--
-- [확인 포인트 — 버전별 예상 관찰]
--   * V3(원자): 단일 게시글에 부하가 몰리면 같은 PK 행에 UPDATE 가 직렬화된다.
--               계단 부하에서 rate 를 올릴수록 Innodb_row_lock_waits 델타와
--               row_lock_time_max 가 커지는 지점이 곧 한계선이다.
--   * V4(급상승): 감지 전에는 V3 와 같은 그림이다. 전환 후에는 증가가 Redis 로
--               빠지고 MySQL 쓰기가 flush 주기당 1건으로 접히므로,
--               post_view_count 의 대기가 급감해야 한다. "전환 전후로 같은 부하에서
--               대기가 사라진다"는 것이 이 파일로 잡아야 할 그림이다.
--   * view_surge_tracking: 감지·연장 UPSERT 와 정리 DELETE 가 부딪히는 곳이다.
--               여기에 대기 체인이 길게 잡히면 감지 로직 자체가 병목이라는 뜻이다.
-- ---------------------------------------------------------------------------

-- [1] 지금 이 순간의 락 대기 체인 (누가 누구를 기다리는가)
SELECT *
FROM sys.innodb_lock_waits;

-- [2] 원시 락 대기 (요청 트랜잭션 / 보유 트랜잭션 쌍)
SELECT
    REQUESTING_ENGINE_TRANSACTION_ID AS waiting_trx,
    BLOCKING_ENGINE_TRANSACTION_ID   AS blocking_trx
FROM performance_schema.data_lock_waits;

-- [3] 조회수·급상승 추적 테이블에 걸려 있는 락 목록
SELECT
    ENGINE_TRANSACTION_ID,
    OBJECT_NAME,
    LOCK_TYPE,
    LOCK_MODE,
    LOCK_STATUS,
    LOCK_DATA
FROM performance_schema.data_locks
WHERE OBJECT_NAME IN ('post_view_count', 'post_view_count_optimistic', 'view_surge_tracking');

-- [4] 누적 카운터 (부하 전/후로 실행해 델타를 계산한다)
--   Innodb_row_lock_waits       : 락 대기가 발생한 횟수
--   Innodb_row_lock_time        : 락 대기에 쓴 총 시간(ms)
--   Innodb_row_lock_time_avg/max: 평균/최대 대기 시간(ms)
SHOW GLOBAL STATUS LIKE 'Innodb_row_lock%';
