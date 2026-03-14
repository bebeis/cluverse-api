-- ============================================================
-- University seed
-- Target: 30 universities
-- Range : university_id 1001 ~ 1030
-- ============================================================

SET @UNIVERSITY_START_ID = 1001;
SET @UNIVERSITY_COUNT = 30;
SET @UNIVERSITY_END_ID = @UNIVERSITY_START_ID + @UNIVERSITY_COUNT - 1;

DELETE FROM university
WHERE university_id BETWEEN @UNIVERSITY_START_ID AND @UNIVERSITY_END_ID;

INSERT INTO university (
    university_id,
    name,
    email_domain,
    badge_image_url,
    address,
    is_active,
    created_at,
    updated_at
) VALUES
    (1001, 'Seed University 01', 'seed01.ac.kr', 'https://cdn.seed.local/university/01.png', 'Seed City 01', TRUE, NOW(), NOW()),
    (1002, 'Seed University 02', 'seed02.ac.kr', 'https://cdn.seed.local/university/02.png', 'Seed City 02', TRUE, NOW(), NOW()),
    (1003, 'Seed University 03', 'seed03.ac.kr', 'https://cdn.seed.local/university/03.png', 'Seed City 03', TRUE, NOW(), NOW()),
    (1004, 'Seed University 04', 'seed04.ac.kr', 'https://cdn.seed.local/university/04.png', 'Seed City 04', TRUE, NOW(), NOW()),
    (1005, 'Seed University 05', 'seed05.ac.kr', 'https://cdn.seed.local/university/05.png', 'Seed City 05', TRUE, NOW(), NOW()),
    (1006, 'Seed University 06', 'seed06.ac.kr', 'https://cdn.seed.local/university/06.png', 'Seed City 06', TRUE, NOW(), NOW()),
    (1007, 'Seed University 07', 'seed07.ac.kr', 'https://cdn.seed.local/university/07.png', 'Seed City 07', TRUE, NOW(), NOW()),
    (1008, 'Seed University 08', 'seed08.ac.kr', 'https://cdn.seed.local/university/08.png', 'Seed City 08', TRUE, NOW(), NOW()),
    (1009, 'Seed University 09', 'seed09.ac.kr', 'https://cdn.seed.local/university/09.png', 'Seed City 09', TRUE, NOW(), NOW()),
    (1010, 'Seed University 10', 'seed10.ac.kr', 'https://cdn.seed.local/university/10.png', 'Seed City 10', TRUE, NOW(), NOW()),
    (1011, 'Seed University 11', 'seed11.ac.kr', 'https://cdn.seed.local/university/11.png', 'Seed City 11', TRUE, NOW(), NOW()),
    (1012, 'Seed University 12', 'seed12.ac.kr', 'https://cdn.seed.local/university/12.png', 'Seed City 12', TRUE, NOW(), NOW()),
    (1013, 'Seed University 13', 'seed13.ac.kr', 'https://cdn.seed.local/university/13.png', 'Seed City 13', TRUE, NOW(), NOW()),
    (1014, 'Seed University 14', 'seed14.ac.kr', 'https://cdn.seed.local/university/14.png', 'Seed City 14', TRUE, NOW(), NOW()),
    (1015, 'Seed University 15', 'seed15.ac.kr', 'https://cdn.seed.local/university/15.png', 'Seed City 15', TRUE, NOW(), NOW()),
    (1016, 'Seed University 16', 'seed16.ac.kr', 'https://cdn.seed.local/university/16.png', 'Seed City 16', TRUE, NOW(), NOW()),
    (1017, 'Seed University 17', 'seed17.ac.kr', 'https://cdn.seed.local/university/17.png', 'Seed City 17', TRUE, NOW(), NOW()),
    (1018, 'Seed University 18', 'seed18.ac.kr', 'https://cdn.seed.local/university/18.png', 'Seed City 18', TRUE, NOW(), NOW()),
    (1019, 'Seed University 19', 'seed19.ac.kr', 'https://cdn.seed.local/university/19.png', 'Seed City 19', TRUE, NOW(), NOW()),
    (1020, 'Seed University 20', 'seed20.ac.kr', 'https://cdn.seed.local/university/20.png', 'Seed City 20', TRUE, NOW(), NOW()),
    (1021, 'Seed University 21', 'seed21.ac.kr', 'https://cdn.seed.local/university/21.png', 'Seed City 21', TRUE, NOW(), NOW()),
    (1022, 'Seed University 22', 'seed22.ac.kr', 'https://cdn.seed.local/university/22.png', 'Seed City 22', TRUE, NOW(), NOW()),
    (1023, 'Seed University 23', 'seed23.ac.kr', 'https://cdn.seed.local/university/23.png', 'Seed City 23', TRUE, NOW(), NOW()),
    (1024, 'Seed University 24', 'seed24.ac.kr', 'https://cdn.seed.local/university/24.png', 'Seed City 24', TRUE, NOW(), NOW()),
    (1025, 'Seed University 25', 'seed25.ac.kr', 'https://cdn.seed.local/university/25.png', 'Seed City 25', TRUE, NOW(), NOW()),
    (1026, 'Seed University 26', 'seed26.ac.kr', 'https://cdn.seed.local/university/26.png', 'Seed City 26', TRUE, NOW(), NOW()),
    (1027, 'Seed University 27', 'seed27.ac.kr', 'https://cdn.seed.local/university/27.png', 'Seed City 27', TRUE, NOW(), NOW()),
    (1028, 'Seed University 28', 'seed28.ac.kr', 'https://cdn.seed.local/university/28.png', 'Seed City 28', TRUE, NOW(), NOW()),
    (1029, 'Seed University 29', 'seed29.ac.kr', 'https://cdn.seed.local/university/29.png', 'Seed City 29', TRUE, NOW(), NOW()),
    (1030, 'Seed University 30', 'seed30.ac.kr', 'https://cdn.seed.local/university/30.png', 'Seed City 30', TRUE, NOW(), NOW());

ALTER TABLE university AUTO_INCREMENT = 1031;
