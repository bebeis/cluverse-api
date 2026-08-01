-- 각 결과가 0행이어야 한다.
SELECT provider, source_fingerprint, COUNT(*) AS duplicate_count
FROM place
GROUP BY provider, source_fingerprint
HAVING COUNT(*) > 1;

SELECT member_id, client_request_id, COUNT(DISTINCT post_id) AS content_count
FROM post
WHERE client_request_id IS NOT NULL
GROUP BY member_id, client_request_id
HAVING COUNT(DISTINCT post_id) > 1;

SELECT member_id, client_request_id, COUNT(DISTINCT comment_id) AS content_count
FROM comment
WHERE client_request_id IS NOT NULL
GROUP BY member_id, client_request_id
HAVING COUNT(DISTINCT comment_id) > 1;

SELECT pp.post_place_id
FROM post_place pp
LEFT JOIN post po ON po.post_id = pp.post_id
LEFT JOIN place p ON p.place_id = pp.place_id
WHERE po.post_id IS NULL OR p.place_id IS NULL;

SELECT cp.comment_place_id
FROM comment_place cp
LEFT JOIN comment co ON co.comment_id = cp.comment_id
LEFT JOIN place p ON p.place_id = cp.place_id
WHERE co.comment_id IS NULL OR p.place_id IS NULL;

-- 삭제·비공개 콘텐츠 제외 여부는 PlaceQueryRepository 통합 테스트와 API 결과로 검증한다.
