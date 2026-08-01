-- 실제 university_id와 좌표를 확인한 뒤 실행한다.
INSERT INTO university_campus (
    university_id, name, latitude, longitude, local_radius_meter,
    is_active, created_at, updated_at
) VALUES (
    1, '신촌캠퍼스', 37.5657840, 126.9385720, 3000,
    TRUE, NOW(), NOW()
) ON DUPLICATE KEY UPDATE
    latitude = VALUES(latitude),
    longitude = VALUES(longitude),
    local_radius_meter = VALUES(local_radius_meter),
    is_active = TRUE,
    updated_at = NOW();
