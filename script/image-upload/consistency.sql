SELECT status, COUNT(*) AS upload_count
FROM post_image_upload
GROUP BY status
ORDER BY status;

SELECT COUNT(DISTINCT u.post_image_upload_id) AS completed_with_missing_metadata
FROM post_image_upload u
LEFT JOIN post_image_asset a ON a.post_image_upload_id = u.post_image_upload_id
WHERE u.status = 'COMPLETED'
  AND (a.post_image_asset_id IS NULL
    OR a.content_key IS NULL
    OR a.thumbnail_key IS NULL
    OR a.content_bytes IS NULL
    OR a.thumbnail_bytes IS NULL);

SELECT COUNT(*) AS completed_staging_cleanup_pending
FROM post_image_upload
WHERE status = 'COMPLETED'
  AND staging_cleaned = 0;

SELECT COUNT(*) AS stale_unresolved_uploads
FROM post_image_upload
WHERE status IN ('PENDING', 'COMPENSATING')
  AND updated_at < NOW(6) - INTERVAL 3 MINUTE;
