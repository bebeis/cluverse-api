SET @member_id = 9200000000;
SET @board_id = 9200000000;
SET @post_id_min = 9200000001;
SET @post_id_max = 9200004000;

DELETE FROM post_comment_activity WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE comment_place
FROM comment_place
JOIN comment ON comment.comment_id = comment_place.comment_id
WHERE comment.post_id BETWEEN @post_id_min AND @post_id_max;
DELETE comment_like
FROM comment_like
JOIN comment ON comment.comment_id = comment_like.comment_id
WHERE comment.post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM comment WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM popular_post WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post_like WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM bookmark WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post_place WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post_tag WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post_image WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post_view_count WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post_like_count WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post_comment_count WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post_bookmark_count WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM post WHERE post_id BETWEEN @post_id_min AND @post_id_max;
DELETE FROM board_popularity_policy WHERE board_id = @board_id;
DELETE FROM board WHERE board_id = @board_id AND name = '__popularity_inline_benchmark__';
DELETE FROM member WHERE member_id = @member_id AND nickname = '__popularity_inline_benchmark__';

SELECT COUNT(*) AS remaining_posts
FROM post
WHERE post_id BETWEEN @post_id_min AND @post_id_max;
