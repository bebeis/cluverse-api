-- ===========================================================================
-- verify-integrity.sql — 급상승 경로 정합성 검증 (Redis 경유해도 유실 0 입증)
-- ===========================================================================
-- V4 는 조회수를 MySQL 과 Redis 두 곳에 나눠 담는 구간이 생긴다. 그래서 검증식이
-- V3 보다 한 항 늘어난다. 측정 전/후에 이 파일을 떠서 아래 등식을 확인한다.
--
--   k6 성공 요청 수 == (MySQL view_count 증가분) + (Redis view:pending 잔여분)
--
-- 좌변만 보면 "MySQL 이 덜 늘었다"로 보이지만, 그 차이는 아직 flush 되지 않고
-- Redis 에 남아 있는 몫이다. 두 항을 더해 일치하면 전환·flush 과정에서
-- 유실이 없다는 뜻이다.
--
-- 사용법:
--   1) k6 실행 직전: [1] 절을 실행해 기준값(before) 기록
--   2) k6 실행 직후: [1] 절을 다시 실행해 after 기록, [2] 절로 추적 상태 확인,
--      곧바로 아래 redis-cli 로 잔여분 확인 (순서 중요 — 아래 주의 참고)
--   3) 급상승이 완전히 만료된 뒤(TTL+유예 경과) [1]+[2] 를 한 번 더 실행해
--      "Redis 키·추적 행이 사라지고 전액이 MySQL 에 반영됐는지" 최종 확인
--
-- 파라미터:
--   @post_id : 급상승 대상 게시글 (lifecycle 의 HOT_POST_ID 와 같은 값)
--
-- [확인 포인트]
--   * 최종 상태(3단계)에서는 잔여분이 0 이어야 한다. 즉 MySQL 증가분만으로
--     k6 성공 수와 일치해야 한다. 여기서 어긋나면 정리 과정에서 마지막 flush 가
--     누락된 것이다 (view_surge_cleanup_total 과 함께 본다).
--   * 추적 행이 남아 있는데 Redis 키가 없다면 flush 후 삭제 순서 문제,
--     반대로 Redis 키가 남았는데 추적 행이 없다면 고아 키다 — 둘 다 버그 신호.
--   * MySQL 반영이 실패해 Redis 로 되돌린 건수는 view_surge_flush_restored_total
--     에 잡힌다. 이 값이 0 이 아닌데 합계가 맞으면 되돌리기가 제대로 동작한 것이고,
--     합계가 어긋나면 "커밋 여부를 알 수 없어 되돌리지 않은" 한 주기치 유실이다.
--   * activated_at 은 감지 시각이다. 램프 시작 시각과의 차이가 곧 감지 지연이므로
--     [2]절 결과를 결과 파일 표 3-2 에 그대로 옮긴다.
--
-- [주의] 중간 시점(2단계)의 스냅샷은 flush 워커(기본 3초 주기)와 경쟁한다.
--        MySQL SELECT 와 redis-cli GET 사이에 flush 가 끼면 같은 증가분을
--        양쪽에서 세거나(중복) 양쪽에서 놓칠(누락) 수 있다. 중간 검증은 참고용으로만
--        쓰고, 정합성 판정은 부하를 멈춘 뒤 3단계 최종 상태로 한다.
-- ---------------------------------------------------------------------------
SET @post_id = 5999999;

-- [1] 조회수 스냅샷 (before / after 두 번 실행)
SELECT post_id, view_count, updated_at
FROM post_view_count
WHERE post_id = @post_id;

-- [2] 급상승 추적 상태 — 언제 감지됐는지(activated_at), 만료가 연장되고 있는지
SELECT post_id, activated_at, expires_at, NOW() AS now
FROM view_surge_tracking
WHERE post_id = @post_id;

-- [3] 지금 급상승으로 등록된 게시글 전체 (라우팅 캐시가 봐야 할 목록)
--     lifecycle 측정 중에는 HOT_POST_ID 하나만 잡히는 것이 정상이다.
--     배경 트래픽 게시글까지 올라온다면 감지 임계값이 너무 낮은 것이다.
SELECT post_id, expires_at
FROM view_surge_tracking
WHERE expires_at > NOW()
ORDER BY expires_at DESC
LIMIT 50;

-- ---------------------------------------------------------------------------
-- Redis 잔여분 — SQL 로는 읽을 수 없으므로 아래 명령을 별도 셸에서 실행한다.
-- 원격 측정이면 script/aws/tunnel.sh start 가 Redis(6379)를 로컬로 포워딩하므로
-- -h 127.0.0.1 로 그대로 붙는다. bastion 에서 직접 볼 거면 Redis EC2 = 10.0.11.20
-- (bastion 에 redis-cli 가 없으면 sudo dnf install -y redis6).
--
--   # 이 게시글의 아직 flush 되지 않은 증가분
--   # (flush 는 키를 지우지 않고 0 으로 초기화한다 — 정리 전까지는 "0" 이 정상이고,
--   #  nil 이면 아직 전환 전이거나 이미 정리된 것이다)
--   redis-cli -h 127.0.0.1 GET view:pending:5999999
--
--   # 급상승으로 잡힌 키 전체 목록
--   redis-cli -h 127.0.0.1 --scan --pattern 'view:pending:*'
--
--   # 최종 확인 — 정리가 끝났다면 키 자체가 없어야 한다 (EXISTS → 0)
--   redis-cli -h 127.0.0.1 EXISTS view:pending:5999999
--
-- 최종 등식: k6 성공 요청 수 == (after.view_count - before.view_count) + GET 잔여분
-- ---------------------------------------------------------------------------
