-- ===========================================================================
-- 05c_view_count_optimistic_seed.sql — 낙관적 락 조회수 실험 테이블 시드
-- ===========================================================================
-- post_view_count_optimistic 은 조회수 증가 V1(낙관적 락) 성능 비교 전용 테이블로,
-- 운영 경로(게시글 생성 시 createViewCount)는 post_view_count 만 채운다.
-- 사전 적재 없이 부하테스트를 돌리면 첫 요청마다 insert 경합이 발생해 측정이
-- 왜곡되므로, post_view_count 를 그대로 복제해(version=0) 미리 채워 둔다.
--
-- 실행 시점: 05 계열(05 / 05a / 05b) post 시드를 모두 넣은 뒤 마지막에 실행.
--            post_view_count 전체를 복제하므로 어떤 조합이든 자동 커버된다.
-- 멱등성  : DELETE 후 INSERT 방식이라 재실행해도 안전하다.
-- ---------------------------------------------------------------------------

DELETE FROM post_view_count_optimistic;

INSERT INTO post_view_count_optimistic (
    post_id,
    view_count,
    version,
    created_at,
    updated_at
)
SELECT
    post_id,
    view_count,
    0 AS version,
    created_at,
    updated_at
FROM post_view_count;
