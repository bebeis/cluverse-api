ALTER TABLE comment
    ADD COLUMN path VARCHAR(255)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NULL;

UPDATE comment
SET path = CONCAT(
        DATE_FORMAT(created_at, '%Y%m%d%H%i%s'), '-',
        LPAD(comment_id, 20, '0')
    )
WHERE parent_id IS NULL;

UPDATE comment child
JOIN comment parent ON parent.comment_id = child.parent_id
SET child.path = CONCAT(
        parent.path, '/',
        DATE_FORMAT(child.created_at, '%Y%m%d%H%i%s'), '-',
        LPAD(child.comment_id, 20, '0')
    )
WHERE child.depth = 1;

UPDATE comment child
JOIN comment parent ON parent.comment_id = child.parent_id
SET child.path = CONCAT(
        parent.path, '/',
        DATE_FORMAT(child.created_at, '%Y%m%d%H%i%s'), '-',
        LPAD(child.comment_id, 20, '0')
    )
WHERE child.depth = 2;

UPDATE comment child
JOIN comment parent ON parent.comment_id = child.parent_id
SET child.path = CONCAT(
        parent.path, '/',
        DATE_FORMAT(child.created_at, '%Y%m%d%H%i%s'), '-',
        LPAD(child.comment_id, 20, '0')
    )
WHERE child.depth = 3;

UPDATE comment child
JOIN comment parent ON parent.comment_id = child.parent_id
SET child.path = CONCAT(
        parent.path, '/',
        DATE_FORMAT(child.created_at, '%Y%m%d%H%i%s'), '-',
        LPAD(child.comment_id, 20, '0')
    )
WHERE child.depth = 4;

UPDATE comment child
JOIN comment parent ON parent.comment_id = child.parent_id
SET child.path = CONCAT(
        parent.path, '/',
        DATE_FORMAT(child.created_at, '%Y%m%d%H%i%s'), '-',
        LPAD(child.comment_id, 20, '0')
    )
WHERE child.depth = 5;

CREATE INDEX idx_comment_post_path
    ON comment (post_id, path);

CREATE INDEX idx_comment_post_parent_created
    ON comment (post_id, parent_id, created_at, comment_id);
