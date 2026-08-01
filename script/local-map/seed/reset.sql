-- 벤치마크 제목으로 생성한 데이터만 제거한다. 운영 DB에서 실행하지 않는다.
DELETE pp
FROM post_place pp
JOIN post po ON po.post_id = pp.post_id
WHERE po.title LIKE '로컬맵 v_ %' OR po.title = '동시성 검증';

DELETE FROM post
WHERE title LIKE '로컬맵 v_ %' OR title = '동시성 검증';

DELETE p
FROM place p
LEFT JOIN post_place pp ON pp.place_id = p.place_id
LEFT JOIN comment_place cp ON cp.place_id = p.place_id
WHERE pp.post_place_id IS NULL AND cp.comment_place_id IS NULL;
