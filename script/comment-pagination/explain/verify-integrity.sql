SET @post_id = COALESCE(@post_id, 1);

SELECT COUNT(*) AS null_path_count
FROM comment
WHERE post_id = @post_id
  AND path IS NULL;

SELECT post_id, path, COUNT(*) AS duplicate_count
FROM comment
WHERE post_id = @post_id
GROUP BY post_id, path
HAVING COUNT(*) > 1;

SELECT child.comment_id, child.parent_id, child.path, parent.path AS parent_path
FROM comment child
JOIN comment parent ON parent.comment_id = child.parent_id
WHERE child.post_id = @post_id
  AND child.path NOT LIKE CONCAT(parent.path, '/%');

SELECT comment_id, depth, CHAR_LENGTH(path) AS path_length
FROM comment
WHERE post_id = @post_id
  AND (depth > 5 OR CHAR_LENGTH(path) > 255);
