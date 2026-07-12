-- ===========================================================================
-- v2-projection.sql — V2 커버링 인덱스 deferred join: 2단계 프로젝션
-- ===========================================================================
-- v2-ids.sql 이 뽑은 소수(size+1개)의 post_id 로만 7조인 프로젝션을 수행한다.
-- offset 이 아무리 깊어도 프로젝션은 항상 최대 21행에만 붙으므로 비용이 일정하다.
-- (V1 은 이 무거운 조인에 직접 OFFSET 을 걸어 깊은 페이지에서 폭증했다 — v1-list.sql)
--
-- 파라미터:
--   IN (...) : v2-ids.sql 이 반환한 post_id 목록. 아래는 예시 값이며, 실제 측정 시
--              v2-ids 결과로 교체한다. 순서 보존은 애플리케이션단에서 재정렬한다.
-- ---------------------------------------------------------------------------
-- 예시 post_id (핫보드 2001001: post_id 5000001~6000000 범위에서 임의 20개+1)
-- 실제로는 v2-ids.sql 실행 결과를 붙여넣는다.

-- [확인 포인트]
--   * post 접근 type = range (post_id IN (...)) → PRIMARY 키로 21행만 직접 조회
--   * 각 조인(member/profile/count 4종/image)이 PK 또는 커버링 인덱스로 eq_ref
--   * rows 총합이 offset 과 무관하게 "고정"인 것을 확인 (deferred join 의 이득)

EXPLAIN
SELECT
    p.post_id,
    p.title,
    SUBSTRING(p.content, 1, 120) AS content_preview,
    p.category,
    p.is_anonymous,
    p.is_pinned,
    p.created_at,
    p.member_id,
    m.nickname,
    mp.profile_image_url,
    pi.image_url                       AS thumbnail_url,
    COALESCE(pvc.view_count, 0)        AS view_count,
    COALESCE(plc.like_count, 0)        AS like_count,
    COALESCE(pcc.comment_count, 0)     AS comment_count,
    COALESCE(pbc.bookmark_count, 0)    AS bookmark_count
FROM post p
    JOIN      member m                ON m.member_id = p.member_id
    LEFT JOIN member_profile mp       ON mp.member_id = p.member_id
    LEFT JOIN post_image pi           ON pi.post_id = p.post_id AND pi.display_order = 0
    LEFT JOIN post_view_count pvc     ON pvc.post_id = p.post_id
    LEFT JOIN post_like_count plc     ON plc.post_id = p.post_id
    LEFT JOIN post_comment_count pcc  ON pcc.post_id = p.post_id
    LEFT JOIN post_bookmark_count pbc ON pbc.post_id = p.post_id
WHERE p.post_id IN (
    5999999, 5999998, 5999997, 5999996, 5999995,
    5999994, 5999993, 5999992, 5999991, 5999990,
    5999989, 5999988, 5999987, 5999986, 5999985,
    5999984, 5999983, 5999982, 5999981, 5999980, 5999979
)
ORDER BY p.created_at DESC, p.post_id DESC;

EXPLAIN ANALYZE
SELECT
    p.post_id,
    p.title,
    SUBSTRING(p.content, 1, 120) AS content_preview,
    p.category,
    p.is_anonymous,
    p.is_pinned,
    p.created_at,
    p.member_id,
    m.nickname,
    mp.profile_image_url,
    pi.image_url                       AS thumbnail_url,
    COALESCE(pvc.view_count, 0)        AS view_count,
    COALESCE(plc.like_count, 0)        AS like_count,
    COALESCE(pcc.comment_count, 0)     AS comment_count,
    COALESCE(pbc.bookmark_count, 0)    AS bookmark_count
FROM post p
    JOIN      member m                ON m.member_id = p.member_id
    LEFT JOIN member_profile mp       ON mp.member_id = p.member_id
    LEFT JOIN post_image pi           ON pi.post_id = p.post_id AND pi.display_order = 0
    LEFT JOIN post_view_count pvc     ON pvc.post_id = p.post_id
    LEFT JOIN post_like_count plc     ON plc.post_id = p.post_id
    LEFT JOIN post_comment_count pcc  ON pcc.post_id = p.post_id
    LEFT JOIN post_bookmark_count pbc ON pbc.post_id = p.post_id
WHERE p.post_id IN (
    5999999, 5999998, 5999997, 5999996, 5999995,
    5999994, 5999993, 5999992, 5999991, 5999990,
    5999989, 5999988, 5999987, 5999986, 5999985,
    5999984, 5999983, 5999982, 5999981, 5999980, 5999979
)
ORDER BY p.created_at DESC, p.post_id DESC;
