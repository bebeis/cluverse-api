-- ===========================================================================
-- verify-integrity.sql — 조회수 정합성 검증 (갱신 유실 0 입증)
-- ===========================================================================
-- fixed 모드 측정의 전/후에 실행해 view_count 스냅샷을 뜬다.
--   (측정 후 값) - (측정 전 값) == k6 성공 요청 수 (view_count_success_rate 의 성공 카운트)
-- 가 성립하면 해당 버전은 경합 속에서도 갱신 유실이 없다는 뜻이다.
--
-- 사용법:
--   1) k6 실행 직전: 대상 버전의 절을 실행해 기준값 기록
--   2) k6 실행 직후: 같은 절을 다시 실행해 델타 계산
--   3) k6 요약의 성공 요청 수와 대조해 results 표(정합성 열)에 O/X 기록
--
-- 파라미터:
--   @post_id : fixed 모드 대상 게시글 (기본 5999999)
--
-- [확인 포인트]
--   * V1(낙관): version 도 함께 뜬다. version 증가량 == view_count 증가량이며,
--     "재시도 끝에 성공한 요청"까지 포함된 총 커밋 횟수다.
--     k6 의 재시도 소진(500) 요청은 증가분에 포함되지 않아야 맞다.
--   * V2/V3: post_view_count 는 두 버전이 공유하므로, 버전을 바꿔 측정할 때마다
--     전/후 스냅샷을 새로 떠야 한다.
-- ---------------------------------------------------------------------------
SET @post_id = 5999999;

-- V1 (낙관적 락) — post_view_count_optimistic
SELECT post_id, view_count, version, updated_at
FROM post_view_count_optimistic
WHERE post_id = @post_id;

-- V2 / V3 (비관적 락 / 원자적 UPDATE) — post_view_count 공유
SELECT post_id, view_count, updated_at
FROM post_view_count
WHERE post_id = @post_id;
