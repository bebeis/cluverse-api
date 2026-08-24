-- 회원 가입과 학교 검색에 반드시 필요한 참조 데이터.
-- 모든 환경에서 Flyway가 멱등하게 보장하며 성능 측정용 대량 시드와 독립적이다.

INSERT INTO university (
    name, email_domain, badge_image_url, address, is_active, created_at, updated_at
) VALUES
    ('서울대학교', 'snu.ac.kr', NULL, '서울특별시 관악구 관악로 1', TRUE, NOW(), NOW()),
    ('고려대학교', 'korea.ac.kr', NULL, '서울특별시 성북구 안암로 145', TRUE, NOW(), NOW()),
    ('연세대학교', 'yonsei.ac.kr', NULL, '서울특별시 서대문구 연세로 50', TRUE, NOW(), NOW()),
    ('성균관대학교', 'skku.edu', NULL, '서울특별시 종로구 성균관로 25-2', TRUE, NOW(), NOW()),
    ('한양대학교', 'hanyang.ac.kr', NULL, '서울특별시 성동구 왕십리로 222', TRUE, NOW(), NOW()),
    ('경희대학교', 'khu.ac.kr', NULL, '서울특별시 동대문구 경희대로 26', TRUE, NOW(), NOW()),
    ('중앙대학교', 'cau.ac.kr', NULL, '서울특별시 동작구 흑석로 84', TRUE, NOW(), NOW()),
    ('서강대학교', 'sogang.ac.kr', NULL, '서울특별시 마포구 백범로 35', TRUE, NOW(), NOW()),
    ('부산대학교', 'pusan.ac.kr', NULL, '부산광역시 금정구 부산대학로63번길 2', TRUE, NOW(), NOW()),
    ('KAIST', 'kaist.ac.kr', NULL, '대전광역시 유성구 대학로 291', TRUE, NOW(), NOW()),
    ('POSTECH', 'postech.ac.kr', NULL, '경상북도 포항시 남구 청암로 77', TRUE, NOW(), NOW()),
    ('홍익대학교', 'hongik.ac.kr', NULL, '서울특별시 마포구 와우산로 94', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    email_domain = VALUES(email_domain),
    address = VALUES(address),
    is_active = TRUE,
    updated_at = NOW();

INSERT INTO university_campus (
    university_id, name, latitude, longitude, local_radius_meter,
    is_active, created_at, updated_at
)
SELECT
    university.university_id,
    seed.campus_name,
    seed.latitude,
    seed.longitude,
    seed.local_radius_meter,
    TRUE,
    NOW(),
    NOW()
FROM (
    SELECT '서울대학교' AS university_name, '관악캠퍼스' AS campus_name,
           37.4599000 AS latitude, 126.9519000 AS longitude, 3000 AS local_radius_meter
    UNION ALL SELECT '고려대학교', '서울캠퍼스', 37.5895000, 127.0324000, 2500
    UNION ALL SELECT '연세대학교', '신촌캠퍼스', 37.5658000, 126.9386000, 2500
    UNION ALL SELECT '성균관대학교', '인문사회과학캠퍼스', 37.5882000, 126.9936000, 2200
    UNION ALL SELECT '한양대학교', '서울캠퍼스', 37.5572000, 127.0453000, 2500
    UNION ALL SELECT '경희대학교', '서울캠퍼스', 37.5963000, 127.0525000, 2200
    UNION ALL SELECT '중앙대학교', '서울캠퍼스', 37.5051000, 126.9570000, 2200
    UNION ALL SELECT '서강대학교', '신촌캠퍼스', 37.5513000, 126.9408000, 1800
    UNION ALL SELECT '부산대학교', '부산캠퍼스', 35.2320000, 129.0824000, 3000
    UNION ALL SELECT 'KAIST', '대전 본원', 36.3721000, 127.3604000, 3000
    UNION ALL SELECT 'POSTECH', '포항캠퍼스', 36.0138000, 129.3236000, 2500
    UNION ALL SELECT '홍익대학교', '서울캠퍼스', 37.5515000, 126.9249000, 2000
) seed
JOIN university ON university.name = seed.university_name
ON DUPLICATE KEY UPDATE
    latitude = VALUES(latitude),
    longitude = VALUES(longitude),
    local_radius_meter = VALUES(local_radius_meter),
    is_active = TRUE,
    updated_at = NOW();

INSERT INTO terms (
    terms_type, title, content, version, is_required, is_active,
    effective_at, created_at, updated_at
) VALUES
    (
        'SERVICE',
        '클루버스 서비스 이용약관',
        '클루버스의 회원 가입, 커뮤니티 이용, 게시물 관리 및 서비스 운영 기준에 동의합니다.',
        '1.0.0',
        TRUE,
        TRUE,
        '2026-01-01 00:00:00',
        NOW(),
        NOW()
    ),
    (
        'PRIVACY',
        '클루버스 개인정보 처리방침',
        '회원 인증과 서비스 제공에 필요한 개인정보의 수집, 이용, 보관 및 파기 기준에 동의합니다.',
        '1.0.0',
        TRUE,
        TRUE,
        '2026-01-01 00:00:00',
        NOW(),
        NOW()
    ),
    (
        'MARKETING',
        '클루버스 마케팅 정보 수신 동의',
        '서비스 소식, 행사 및 모집 정보 수신에 동의합니다.',
        '1.0.0',
        FALSE,
        TRUE,
        '2026-01-01 00:00:00',
        NOW(),
        NOW()
    )
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    content = VALUES(content),
    is_required = VALUES(is_required),
    is_active = TRUE,
    effective_at = VALUES(effective_at),
    updated_at = NOW();
