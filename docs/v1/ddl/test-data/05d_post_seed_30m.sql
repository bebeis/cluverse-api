-- ============================================================
-- General board long-tail post seed (16,000,000 rows)
-- Target:
--   - post: 16,000,000 rows → board 2000001 ~ 2000120 (05가 만든 일반 게시판)
--   - 05/05a/05b 누적 1,100만 건(post_id 3000001 ~ 14000000)에 이어 붙여
--     post_id 상한 3,000만 / 실 행수 2,700만이 된다.
--     (05b 헤더 주석의 "~13999999"는 오기 — 실제 마지막 id는 14000000이다)
-- Dependencies:
--   - 02_member_seed.sql  (member 1000001 ~ 1050000)
--   - 05_post_seed.sql    (board 2000001 ~ 2000120 — 이 스크립트는 board를 만들지 않는다)
--   - 05a / 05b 와는 무관 (id 범위·게시판이 겹치지 않아 순서 상관없음)
-- Recommended order:
--   - 05 (필수) → [05a → 05b] → 05d
-- Ranges:
--   - board_id : 2000001 ~ 2000120 (재사용, 보드당 약 133,333건 추가)
--   - post_id  : 14000001 ~ 30000000
-- 예상 소요시간:
--   - 05b(800만 건) 실측치 기준의 **추정**이며 실측값이 아니다.
--     행수는 05b의 2배지만 보드 120개 × 기간 2.5년으로 흩뿌리므로
--     세컨더리 인덱스(board_id, status, created_at, post_id) 삽입이 랜덤이 되어
--     단순 2배보다 더 걸린다. 05b 소요의 **약 2.5~3.5배**로 잡을 것.
--     (t3.small + gp3 30GB + 기본 버퍼풀 128MB 기준)
-- 디스크 주의:
--   - post 약 6GB + 카운트 4종 약 3GB ≈ +9GB. MySQL EBS가 30GB이므로
--     05/05a/05b까지 적재된 상태에서 돌리면 여유가 크지 않다.
--     바이너리 로그(MySQL 8 기본 ON)까지 수 GB 쌓이므로 적재 전 `df -h`,
--     적재 후 필요하면 `PURGE BINARY LOGS` 로 회수할 것.
-- Includes:
--   - post
--   - post_view_count
--   - post_like_count
--   - post_comment_count
--   - post_bookmark_count
--   - (post_tag / post_image 는 만들지 않는다 — 목록/조회수 측정에 쓰이지 않아 제외.
--      단, 재실행 멱등성을 위해 해당 범위의 정리(DELETE)는 수행한다)
-- 멱등성:
--   - 14000001 ~ 30000000 범위를 자식 테이블부터 DELETE 후 INSERT 하므로 재실행 안전.
-- ============================================================

SET @GENERAL_BOARD_START = 2000001;
SET @GENERAL_BOARD_COUNT = 120;

SET @POST30M_START = 14000001;
SET @POST30M_COUNT = 16000000;
SET @POST30M_END   = @POST30M_START + @POST30M_COUNT - 1;   -- 30000000

-- created_at 분산: 2024-01-01 00:00 ~ 2026-06-30 23:59 (912일 = 1,313,280분)
-- 역주행(오래된 글의 조회수 급등) 시나리오를 위해 NOW() 기준 상대시간이 아니라
-- 고정 앵커에서 결정적으로 계산한다. 7919는 소수라 1,313,280(2^9·3^3·5·19)과 서로소이므로
-- 모든 분(minute) 슬롯에 고르게(슬롯당 12~13건) 떨어진다.
SET @SEED_EPOCH     = CAST('2024-01-01 00:00:00' AS DATETIME);
SET @SPREAD_MINUTES = 1313280;
SET @SPREAD_STEP    = 7919;

-- ------------------------------------------------------------
-- cleanup (100만 id 단위로 쪼갠다 — 1,600만 행 단일 DELETE는 언두 로그가 과하다)
-- ------------------------------------------------------------
DELIMITER $$

DROP PROCEDURE IF EXISTS purge_posts_30m $$
CREATE PROCEDURE purge_posts_30m()
BEGIN
    DECLARE v_from  BIGINT DEFAULT 14000001;
    DECLARE v_to    BIGINT;
    DECLARE v_end   BIGINT DEFAULT 30000000;
    DECLARE v_chunk BIGINT DEFAULT 1000000;

    WHILE v_from <= v_end DO
        SET v_to = LEAST(v_from + v_chunk - 1, v_end);

        DELETE FROM post_tag            WHERE post_id BETWEEN v_from AND v_to;
        DELETE FROM post_image          WHERE post_id BETWEEN v_from AND v_to;
        DELETE FROM post_view_count     WHERE post_id BETWEEN v_from AND v_to;
        DELETE FROM post_like_count     WHERE post_id BETWEEN v_from AND v_to;
        DELETE FROM post_comment_count  WHERE post_id BETWEEN v_from AND v_to;
        DELETE FROM post_bookmark_count WHERE post_id BETWEEN v_from AND v_to;
        DELETE FROM post                WHERE post_id BETWEEN v_from AND v_to;

        SET v_from = v_from + v_chunk;
    END WHILE;
END $$

CALL purge_posts_30m() $$
DROP PROCEDURE purge_posts_30m $$

DELIMITER ;

-- ------------------------------------------------------------
-- seq helper table (0 ~ 9999)
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_seed_seq_10000;
CREATE TEMPORARY TABLE tmp_seed_seq_10000
ENGINE=MEMORY
AS
SELECT
    ones.digit
    + tens.digit      * 10
    + hundreds.digit  * 100
    + thousands.digit * 1000 AS seq
FROM (
    SELECT 0 AS digit UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) ones
CROSS JOIN (
    SELECT 0 AS digit UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) tens
CROSS JOIN (
    SELECT 0 AS digit UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) hundreds
CROSS JOIN (
    SELECT 0 AS digit UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) thousands;

ALTER TABLE tmp_seed_seq_10000 ADD PRIMARY KEY (seq);

DELIMITER $$

-- ------------------------------------------------------------
-- post: 16,000,000 rows (post_id 14000001 ~ 30000000)
--
-- status/is_pinned 비율은 05_post_seed.sql 과 같지만 **위상만** 옮겼다
-- (MOD(n,1000)=0 → =500 등). 05a에서 경계 id 6000000이 DELETED로 잡혀
-- 벤치 기본 대상을 5999999로 바꿔야 했던 함정을 반복하지 않기 위함으로,
-- 이 시드의 마지막 id 30000000 과 그 앞 29999999 는 모두 ACTIVE + 미고정이다.
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS seed_posts_30m $$
CREATE PROCEDURE seed_posts_30m()
BEGIN
    DECLARE v_offset     INT    DEFAULT 0;
    DECLARE v_batch_size INT    DEFAULT 10000;
    DECLARE v_total      INT    DEFAULT 16000000;
    DECLARE v_start      BIGINT DEFAULT 14000001;

    WHILE v_offset < v_total DO
        INSERT INTO post (
            post_id, board_id, member_id, title, content, category,
            is_anonymous, is_pinned, is_external_visible, status,
            deleted_at, client_ip, created_at, updated_at
        )
        SELECT
            v_start + v_offset + s.seq,
            @GENERAL_BOARD_START + MOD(v_offset + s.seq, @GENERAL_BOARD_COUNT),
            1000001 + MOD((v_offset + s.seq) * 7, 50000),
            CONCAT('Seed post ', LPAD(v_start + v_offset + s.seq, 8, '0')),
            CONCAT(
                'Generated content for seed post ',
                LPAD(v_start + v_offset + s.seq, 8, '0'),
                '. This row extends the general boards to 30M rows for long-tail and view-surge testing.'
            ),
            CASE MOD(v_offset + s.seq, 7)
                WHEN 0 THEN 'NOTICE'
                WHEN 1 THEN 'GENERAL'
                WHEN 2 THEN 'QUESTION'
                WHEN 3 THEN 'INFORMATION'
                WHEN 4 THEN 'REVIEW'
                WHEN 5 THEN 'RESOURCE'
                ELSE 'RECRUITMENT'
            END,
            MOD(v_offset + s.seq + 1, 11) = 0,
            MOD(v_offset + s.seq + 1, 5000) = 2500,
            MOD(v_offset + s.seq + 1, 13) <> 0,
            CASE
                WHEN MOD(v_offset + s.seq + 1, 1000) = 500 THEN 'DELETED'
                WHEN MOD(v_offset + s.seq + 1, 300)  = 150 THEN 'BLINDED'
                ELSE 'ACTIVE'
            END,
            CASE
                WHEN MOD(v_offset + s.seq + 1, 1000) = 500
                THEN DATE_ADD(s.c_at, INTERVAL MOD(v_offset + s.seq, 90) DAY)
                ELSE NULL
            END,
            CONCAT(
                '172.', MOD(v_offset + s.seq, 250), '.',
                MOD(FLOOR((v_offset + s.seq) / 250), 250), '.',
                MOD((v_offset + s.seq) * 3, 250)
            ),
            s.c_at,
            s.c_at
        FROM (
            SELECT
                seq,
                DATE_ADD(
                    @SEED_EPOCH,
                    INTERVAL MOD((v_offset + seq) * @SPREAD_STEP, @SPREAD_MINUTES) MINUTE
                ) AS c_at
            FROM tmp_seed_seq_10000
            WHERE seq < LEAST(v_batch_size, v_total - v_offset)
        ) s;

        SET v_offset = v_offset + v_batch_size;
    END WHILE;
END $$

CALL seed_posts_30m() $$
DROP PROCEDURE seed_posts_30m $$

-- ------------------------------------------------------------
-- 카운트 4종: 새 post 전 건에 1행씩 (05a/05b 방식 — 05와 달리 0건도 행을 만든다)
-- 값 분포 공식은 05_post_seed.sql 과 동일. 100만 id 단위로 끊어
-- 트랜잭션당 크기를 제한하고, 같은 청크의 post 페이지를 4번 재사용한다.
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS seed_post_counts_30m $$
CREATE PROCEDURE seed_post_counts_30m()
BEGIN
    DECLARE v_from  BIGINT DEFAULT 14000001;
    DECLARE v_to    BIGINT;
    DECLARE v_end   BIGINT DEFAULT 30000000;
    DECLARE v_chunk BIGINT DEFAULT 1000000;

    WHILE v_from <= v_end DO
        SET v_to = LEAST(v_from + v_chunk - 1, v_end);

        INSERT INTO post_view_count (post_id, view_count, created_at, updated_at)
        SELECT
            post_id,
            MOD((post_id - @POST30M_START + 1) * 17, 20000),
            created_at,
            updated_at
        FROM post
        WHERE post_id BETWEEN v_from AND v_to;

        INSERT INTO post_like_count (post_id, like_count, created_at, updated_at)
        SELECT
            post_id,
            MOD((post_id - @POST30M_START + 1) * 7, 300),
            created_at,
            updated_at
        FROM post
        WHERE post_id BETWEEN v_from AND v_to;

        INSERT INTO post_comment_count (post_id, comment_count, created_at, updated_at)
        SELECT
            post_id,
            MOD((post_id - @POST30M_START + 1) * 13, 80),
            created_at,
            updated_at
        FROM post
        WHERE post_id BETWEEN v_from AND v_to;

        INSERT INTO post_bookmark_count (post_id, bookmark_count, created_at, updated_at)
        SELECT
            post_id,
            MOD((post_id - @POST30M_START + 1) * 3, 120),
            created_at,
            updated_at
        FROM post
        WHERE post_id BETWEEN v_from AND v_to;

        SET v_from = v_from + v_chunk;
    END WHILE;
END $$

CALL seed_post_counts_30m() $$
DROP PROCEDURE seed_post_counts_30m $$

DELIMITER ;

-- ------------------------------------------------------------
-- AUTO_INCREMENT reset
-- ------------------------------------------------------------
ALTER TABLE post AUTO_INCREMENT = 30000001;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_seq_10000;

ANALYZE TABLE post, post_view_count, post_like_count, post_comment_count, post_bookmark_count;
