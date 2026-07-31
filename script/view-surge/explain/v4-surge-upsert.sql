-- ===========================================================================
-- v4-surge-upsert.sql — 급상승 감지 시 추적 테이블 등록(UPSERT) EXPLAIN
-- ===========================================================================
-- 관측 창 안에서 조회 속도가 임계값을 넘으면 해당 게시글을 view_surge_tracking 에
-- 등록한다. 이미 급상승 중인 글이면 만료 시각만 뒤로 민다 — 이때 GREATEST 를 쓰는
-- 이유는, 여러 앱 인스턴스가 각자 계산한 만료 시각으로 동시에 UPSERT 할 때
-- 늦게 도착한 짧은 만료가 이미 연장된 만료를 되돌리는 것을 막기 위해서다.
--
-- 파라미터:
--   @post_id      : 급상승으로 감지된 게시글
--   @activated_at : 감지 시각
--   @expires_at   : 이번 감지 기준 만료 시각 (감지 시각 + tracking-ttl, 기본 5분)
--
-- [확인 포인트]
--   * PK(post_id) 충돌 판정이므로 인덱스 추가 탐색 없이 단건 접근이어야 한다.
--   * EXPLAIN 은 INSERT 계열에서 출력이 제한적이다(실행하지 않고 플랜만 본다).
--     여기서 볼 것은 "충돌 판정에 PRIMARY 를 쓰는가" 하나다. 실제 비용은
--     감지가 몰릴 때의 쓰기 경합이며, 그건 lock-waits.sql 로 관찰한다.
--   * 등록 자체는 급상승 게시글 수만큼만 발생한다(요청마다가 아니다).
--     부하 중 이 테이블의 행 수가 요청량에 비례해 늘어난다면 감지 조건이
--     너무 헐거운 것이므로 view-surge.threshold 를 재검토해야 한다.
--   * [B] 의 연장 UPDATE 는 GREATEST 없이 그대로 덮어쓴다. flush 워커 한 곳에서만
--     호출하는 경로라 되감기 경합이 없다는 전제다 — 인스턴스가 늘어 이 전제가
--     깨지면 [A] 처럼 GREATEST 가 필요해진다.
-- ---------------------------------------------------------------------------
SET @post_id = 5999999;
SET @activated_at = NOW();
SET @expires_at = NOW() + INTERVAL 5 MINUTE;

-- [A] 감지 등록 UPSERT (ViewSurgeTrackingRepository.upsertActivation)
EXPLAIN
INSERT INTO view_surge_tracking (post_id, activated_at, expires_at)
VALUES (@post_id, @activated_at, @expires_at)
ON DUPLICATE KEY UPDATE expires_at = GREATEST(expires_at, @expires_at);

EXPLAIN FORMAT=TREE
INSERT INTO view_surge_tracking (post_id, activated_at, expires_at)
VALUES (@post_id, @activated_at, @expires_at)
ON DUPLICATE KEY UPDATE expires_at = GREATEST(expires_at, @expires_at);

-- [B] 만료 연장 UPDATE (flush 델타가 sustain-threshold 이상일 때만 호출)
EXPLAIN
UPDATE view_surge_tracking
SET expires_at = @expires_at
WHERE post_id = @post_id;
