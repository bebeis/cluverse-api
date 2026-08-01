-- 같은 MySQL 세션에서 [1] 기준 스냅샷 → 부하 → [2] 델타 조회 순서로 실행한다.
-- performance_schema와 statements_digest consumer가 켜져 있어야 한다.

-- [1] 부하 직전
DROP TEMPORARY TABLE IF EXISTS popularity_digest_before;
CREATE TEMPORARY TABLE popularity_digest_before AS
SELECT DIGEST,
       COUNT_STAR,
       SUM_TIMER_WAIT,
       SUM_ROWS_EXAMINED,
       SUM_ROWS_SENT,
       SUM_NO_INDEX_USED,
       SUM_CREATED_TMP_DISK_TABLES
FROM performance_schema.events_statements_summary_by_digest
WHERE SCHEMA_NAME = DATABASE()
  AND (
       DIGEST_TEXT LIKE '%POPULAR_POST%'
    OR DIGEST_TEXT LIKE '%POPULARITY_CANDIDATE%'
    OR DIGEST_TEXT LIKE '%BOARD_POPULARITY_POLICY%'
    OR DIGEST_TEXT LIKE '%POST_LIKE_COUNT%'
    OR DIGEST_TEXT LIKE '%POST_COMMENT_COUNT%'
    OR DIGEST_TEXT LIKE '%POST_VIEW_COUNT%'
  );

-- 이 지점에서 k6 부하를 실행한다.

-- [2] 부하 직후. wait_ms는 해당 digest 전체 실행 시간의 합이다.
SELECT LEFT(after_stat.DIGEST_TEXT, 180) AS digest_text,
       after_stat.COUNT_STAR - COALESCE(before_stat.COUNT_STAR, 0) AS executions,
       ROUND(
           (after_stat.SUM_TIMER_WAIT - COALESCE(before_stat.SUM_TIMER_WAIT, 0)) / 1000000000,
           3
       ) AS wait_ms,
       after_stat.SUM_ROWS_EXAMINED - COALESCE(before_stat.SUM_ROWS_EXAMINED, 0) AS rows_examined,
       after_stat.SUM_ROWS_SENT - COALESCE(before_stat.SUM_ROWS_SENT, 0) AS rows_sent,
       after_stat.SUM_NO_INDEX_USED - COALESCE(before_stat.SUM_NO_INDEX_USED, 0) AS no_index_used,
       after_stat.SUM_CREATED_TMP_DISK_TABLES
           - COALESCE(before_stat.SUM_CREATED_TMP_DISK_TABLES, 0) AS tmp_disk_tables
FROM performance_schema.events_statements_summary_by_digest after_stat
LEFT JOIN popularity_digest_before before_stat ON before_stat.DIGEST = after_stat.DIGEST
WHERE after_stat.SCHEMA_NAME = DATABASE()
  AND (
       after_stat.DIGEST_TEXT LIKE '%POPULAR_POST%'
    OR after_stat.DIGEST_TEXT LIKE '%POPULARITY_CANDIDATE%'
    OR after_stat.DIGEST_TEXT LIKE '%BOARD_POPULARITY_POLICY%'
    OR after_stat.DIGEST_TEXT LIKE '%POST_LIKE_COUNT%'
    OR after_stat.DIGEST_TEXT LIKE '%POST_COMMENT_COUNT%'
    OR after_stat.DIGEST_TEXT LIKE '%POST_VIEW_COUNT%'
  )
HAVING executions > 0
ORDER BY wait_ms DESC;
