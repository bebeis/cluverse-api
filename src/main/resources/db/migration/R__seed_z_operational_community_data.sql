-- 운영 화면이 빈 서비스처럼 보이지 않도록 보장하는 최소 커뮤니티 데이터.
-- 성능 측정용 대량 시드와 분리하며, 예약 ID 대역의 행만 멱등하게 관리한다.

SET @COMMUNITY_SEED_NOW = NOW();

INSERT INTO member (
    member_id, nickname, university_id, status, verification_status, role,
    last_login_at, source_system, created_at, updated_at
) VALUES
    (920001, '민준.dev', (SELECT university_id FROM university WHERE name = '서울대학교'), 'ACTIVE', 'APPROVED', 'MEMBER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 2 HOUR), 'WEB_USER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 140 DAY), @COMMUNITY_SEED_NOW),
    (920002, '서연PM', (SELECT university_id FROM university WHERE name = '연세대학교'), 'ACTIVE', 'APPROVED', 'MEMBER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 6 HOUR), 'WEB_USER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 132 DAY), @COMMUNITY_SEED_NOW),
    (920003, '도현회로', (SELECT university_id FROM university WHERE name = '고려대학교'), 'ACTIVE', 'APPROVED', 'MEMBER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 1 DAY), 'WEB_USER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 128 DAY), @COMMUNITY_SEED_NOW),
    (920004, '하은리서치', (SELECT university_id FROM university WHERE name = '중앙대학교'), 'ACTIVE', 'APPROVED', 'MEMBER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 12 HOUR), 'WEB_USER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 121 DAY), @COMMUNITY_SEED_NOW),
    (920005, '지우창업', (SELECT university_id FROM university WHERE name = 'KAIST'), 'ACTIVE', 'APPROVED', 'MEMBER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 4 HOUR), 'WEB_USER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 119 DAY), @COMMUNITY_SEED_NOW),
    (920006, '예준밴드', (SELECT university_id FROM university WHERE name = '한양대학교'), 'ACTIVE', 'APPROVED', 'MEMBER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 10 HOUR), 'WEB_USER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 116 DAY), @COMMUNITY_SEED_NOW),
    (920007, '윤서풋살', (SELECT university_id FROM university WHERE name = '경희대학교'), 'ACTIVE', 'APPROVED', 'MEMBER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 5 HOUR), 'WEB_USER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 112 DAY), @COMMUNITY_SEED_NOW),
    (920008, '현우데이터', (SELECT university_id FROM university WHERE name = 'POSTECH'), 'ACTIVE', 'APPROVED', 'MEMBER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 8 HOUR), 'WEB_USER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 108 DAY), @COMMUNITY_SEED_NOW),
    (920009, '채원마케터', (SELECT university_id FROM university WHERE name = '성균관대학교'), 'ACTIVE', 'APPROVED', 'MEMBER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 9 HOUR), 'WEB_USER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 104 DAY), @COMMUNITY_SEED_NOW),
    (920010, '수아UX', (SELECT university_id FROM university WHERE name = '홍익대학교'), 'ACTIVE', 'APPROVED', 'MEMBER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 3 HOUR), 'WEB_USER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 99 DAY), @COMMUNITY_SEED_NOW),
    (920011, '태윤러너', (SELECT university_id FROM university WHERE name = '서강대학교'), 'ACTIVE', 'APPROVED', 'MEMBER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 11 HOUR), 'WEB_USER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 95 DAY), @COMMUNITY_SEED_NOW),
    (920012, '가을백엔드', (SELECT university_id FROM university WHERE name = '부산대학교'), 'ACTIVE', 'APPROVED', 'MEMBER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 1 HOUR), 'WEB_USER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 88 DAY), @COMMUNITY_SEED_NOW)
ON DUPLICATE KEY UPDATE
    university_id = VALUES(university_id), status = 'ACTIVE', verification_status = 'APPROVED', updated_at = @COMMUNITY_SEED_NOW;

INSERT INTO member_profile (
    member_id, bio, entrance_year, profile_image_url, is_public, visible_fields, created_at, updated_at
) VALUES
    (920001, '컴퓨터공학을 전공하며 Spring과 분산 시스템을 공부하고 있습니다.', 2021, 'https://i.pravatar.cc/150?u=cluverse-920001', TRUE, JSON_OBJECT('bio', TRUE), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 140 DAY), @COMMUNITY_SEED_NOW),
    (920002, '사이드 프로젝트에서 PM을 맡고 사용자 인터뷰를 기록합니다.', 2021, 'https://i.pravatar.cc/150?u=cluverse-920002', TRUE, JSON_OBJECT('bio', TRUE), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 132 DAY), @COMMUNITY_SEED_NOW),
    (920003, '임베디드와 로봇 제어를 좋아하는 전자전기 전공자입니다.', 2020, 'https://i.pravatar.cc/150?u=cluverse-920003', TRUE, JSON_OBJECT('bio', TRUE), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 128 DAY), @COMMUNITY_SEED_NOW),
    (920004, '사용자 리서치와 인터뷰 설계에 관심이 많습니다.', 2022, 'https://i.pravatar.cc/150?u=cluverse-920004', TRUE, JSON_OBJECT('bio', TRUE), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 121 DAY), @COMMUNITY_SEED_NOW),
    (920005, 'AI 제품의 시장 검증과 초기 팀 빌딩을 경험하고 있습니다.', 2021, 'https://i.pravatar.cc/150?u=cluverse-920005', TRUE, JSON_OBJECT('bio', TRUE), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 119 DAY), @COMMUNITY_SEED_NOW),
    (920006, '웹 개발과 밴드 보컬을 함께 하고 있습니다.', 2020, 'https://i.pravatar.cc/150?u=cluverse-920006', TRUE, JSON_OBJECT('bio', TRUE), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 116 DAY), @COMMUNITY_SEED_NOW),
    (920007, '풋살 번개와 교내 리그 정보를 자주 공유합니다.', 2022, 'https://i.pravatar.cc/150?u=cluverse-920007', TRUE, JSON_OBJECT('bio', TRUE), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 112 DAY), @COMMUNITY_SEED_NOW),
    (920008, '논문 읽기 모임과 데이터 파이프라인에 관심이 큽니다.', 2021, 'https://i.pravatar.cc/150?u=cluverse-920008', TRUE, JSON_OBJECT('bio', TRUE), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 108 DAY), @COMMUNITY_SEED_NOW),
    (920009, '브랜딩과 콘텐츠 전략을 공부하고 있습니다.', 2021, 'https://i.pravatar.cc/150?u=cluverse-920009', TRUE, JSON_OBJECT('bio', TRUE), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 104 DAY), @COMMUNITY_SEED_NOW),
    (920010, 'UX와 포트폴리오 피드백 모임을 운영합니다.', 2022, 'https://i.pravatar.cc/150?u=cluverse-920010', TRUE, JSON_OBJECT('bio', TRUE), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 99 DAY), @COMMUNITY_SEED_NOW),
    (920011, '주말 러닝 번개를 열고 코스를 기록합니다.', 2023, 'https://i.pravatar.cc/150?u=cluverse-920011', TRUE, JSON_OBJECT('bio', TRUE), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 95 DAY), @COMMUNITY_SEED_NOW),
    (920012, '부산에서 백엔드 스터디를 꾸준히 하고 있습니다.', 2020, 'https://i.pravatar.cc/150?u=cluverse-920012', TRUE, JSON_OBJECT('bio', TRUE), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 88 DAY), @COMMUNITY_SEED_NOW)
ON DUPLICATE KEY UPDATE
    bio = VALUES(bio), entrance_year = VALUES(entrance_year), profile_image_url = VALUES(profile_image_url), is_public = TRUE, updated_at = @COMMUNITY_SEED_NOW;

INSERT INTO board (
    board_id, board_type, name, description, parent_id, depth, display_order, is_active, created_at, updated_at
) VALUES
    (930001, 'DEPARTMENT', '공학계열', '공학 전공 수업과 진로를 함께 이야기합니다.', NULL, 0, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (930002, 'DEPARTMENT', '경상·사회계열', '비즈니스와 사회과학 전공 정보를 나눕니다.', NULL, 0, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (930003, 'DEPARTMENT', '예술·체육계열', '창작과 스포츠 전공의 작업 및 활동을 공유합니다.', NULL, 0, 3, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (930004, 'DEPARTMENT', '컴퓨터공학과', '백엔드, 시스템, 알고리즘 중심 전공 보드', 930001, 1, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930005, 'DEPARTMENT', '소프트웨어학부', '웹과 앱 서비스 개발 중심 전공 보드', 930001, 1, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930006, 'DEPARTMENT', '전자전기공학부', '회로, 임베디드, 로봇 시스템 중심 전공 보드', 930001, 1, 3, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930007, 'DEPARTMENT', '인공지능학과', '모델 실험, 논문, 데이터셋 중심 전공 보드', 930001, 1, 4, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930008, 'DEPARTMENT', '경영학과', '프로덕트, 조직, 창업 관련 전공 보드', 930002, 1, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930009, 'DEPARTMENT', '경제학부', '산업 분석, 금융, 데이터 해석 중심 전공 보드', 930002, 1, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930010, 'DEPARTMENT', '심리학과', '사용자 리서치와 행동 이해 중심 전공 보드', 930002, 1, 3, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930011, 'DEPARTMENT', '시각디자인학과', '브랜딩, UI, 포스터 작업 중심 전공 보드', 930003, 1, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930012, 'DEPARTMENT', '스포츠과학과', '러닝과 구기 종목, 팀 운영 중심 전공 보드', 930003, 1, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930013, 'DEPARTMENT', '미디어커뮤니케이션학과', '영상 제작과 콘텐츠 기획 중심 전공 보드', 930003, 1, 3, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930014, 'INTEREST', '개발', '개발자 커뮤니티', NULL, 0, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (930015, 'INTEREST', '창업·기획', '스타트업과 제품 기획 커뮤니티', NULL, 0, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (930016, 'INTEREST', '문화·예술', '음악, 미술, 영상 커뮤니티', NULL, 0, 3, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (930017, 'INTEREST', '스포츠', '풋살과 러닝 커뮤니티', NULL, 0, 4, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (930018, 'INTEREST', '커리어', '취업 준비와 포트폴리오 커뮤니티', NULL, 0, 5, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (930019, 'INTEREST', '언어교환', '외국어 회화와 교류 커뮤니티', NULL, 0, 6, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (930020, 'INTEREST', '백엔드', '서버 개발과 아키텍처 이야기', 930014, 1, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930021, 'INTEREST', '프론트엔드', '웹 UI와 인터랙션 이야기', 930014, 1, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930022, 'INTEREST', 'AI·데이터', '모델링과 데이터 처리 이야기', 930014, 1, 3, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930023, 'INTEREST', '스타트업', '시장 검증과 팀 빌딩 이야기', 930015, 1, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930024, 'INTEREST', '브랜딩', '브랜드 메시지와 비주얼 이야기', 930015, 1, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930025, 'INTEREST', '밴드', '합주, 공연, 장비 이야기', 930016, 1, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930026, 'INTEREST', '드로잉·미술', '크로키, 전시, 작업 이야기', 930016, 1, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930027, 'INTEREST', '축구·풋살', '경기 모집과 구장 정보', 930017, 1, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930028, 'INTEREST', '러닝', '코스와 러닝 번개 정보', 930017, 1, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930029, 'INTEREST', '취업스터디', '채용 정보와 면접 준비', 930018, 1, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930030, 'INTEREST', '포트폴리오', '직무별 포트폴리오 피드백', 930018, 1, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930031, 'INTEREST', '영어회화', '영어 회화 모임과 자료', 930019, 1, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930032, 'INTEREST', '일본어회화', '일본어 회화 모임과 자료', 930019, 1, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (930033, 'GROUP', '대학연합 사이드프로젝트 랩', '기획부터 배포까지 함께하는 프로젝트 팀', NULL, 0, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 100 DAY), @COMMUNITY_SEED_NOW),
    (930034, 'GROUP', '안암 밴드클럽', '정기 합주와 학기말 공연을 준비하는 밴드', NULL, 0, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 95 DAY), @COMMUNITY_SEED_NOW),
    (930035, 'GROUP', '한강 러닝 메이트', '평일 저녁과 주말에 함께 달리는 모임', NULL, 0, 3, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 90 DAY), @COMMUNITY_SEED_NOW),
    (930036, 'GROUP', 'AI 논문 읽기 모임', '매주 논문 한 편을 읽고 발표하는 온라인 스터디', NULL, 0, 4, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 85 DAY), @COMMUNITY_SEED_NOW)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), description = VALUES(description), parent_id = VALUES(parent_id), depth = VALUES(depth), display_order = VALUES(display_order), is_active = TRUE, updated_at = @COMMUNITY_SEED_NOW;

INSERT INTO major (major_id, board_id, name, parent_id, depth, display_order, is_active, created_at, updated_at) VALUES
    (940001, 930001, '공학계열', NULL, 0, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (940002, 930002, '경상·사회계열', NULL, 0, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (940003, 930003, '예술·체육계열', NULL, 0, 3, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (940004, 930004, '컴퓨터공학과', 940001, 1, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (940005, 930005, '소프트웨어학부', 940001, 1, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (940006, 930006, '전자전기공학부', 940001, 1, 3, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (940007, 930007, '인공지능학과', 940001, 1, 4, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (940008, 930008, '경영학과', 940002, 1, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (940009, 930009, '경제학부', 940002, 1, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (940010, 930010, '심리학과', 940002, 1, 3, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (940011, 930011, '시각디자인학과', 940003, 1, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (940012, 930012, '스포츠과학과', 940003, 1, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (940013, 930013, '미디어커뮤니케이션학과', 940003, 1, 3, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW)
ON DUPLICATE KEY UPDATE
    board_id = VALUES(board_id), name = VALUES(name), parent_id = VALUES(parent_id), depth = VALUES(depth), display_order = VALUES(display_order), is_active = TRUE, updated_at = @COMMUNITY_SEED_NOW;

INSERT INTO interest (interest_id, board_id, name, category, parent_id, display_order, is_active, created_at, updated_at) VALUES
    (950001, 930014, '개발', 'TECH', NULL, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (950002, 930015, '창업·기획', 'BUSINESS', NULL, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (950003, 930016, '문화·예술', 'ART', NULL, 3, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (950004, 930017, '스포츠', 'SPORTS', NULL, 4, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (950005, 930018, '커리어', 'CAREER', NULL, 5, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (950006, 930019, '언어교환', 'LANGUAGE', NULL, 6, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 150 DAY), @COMMUNITY_SEED_NOW),
    (950007, 930020, '백엔드', 'TECH', 950001, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (950008, 930021, '프론트엔드', 'TECH', 950001, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (950009, 930022, 'AI·데이터', 'TECH', 950001, 3, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (950010, 930023, '스타트업', 'BUSINESS', 950002, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (950011, 930024, '브랜딩', 'BUSINESS', 950002, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (950012, 930025, '밴드', 'ART', 950003, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (950013, 930026, '드로잉·미술', 'ART', 950003, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (950014, 930027, '축구·풋살', 'SPORTS', 950004, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (950015, 930028, '러닝', 'SPORTS', 950004, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (950016, 930029, '취업스터디', 'CAREER', 950005, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (950017, 930030, '포트폴리오', 'CAREER', 950005, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (950018, 930031, '영어회화', 'LANGUAGE', 950006, 1, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW),
    (950019, 930032, '일본어회화', 'LANGUAGE', 950006, 2, TRUE, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 145 DAY), @COMMUNITY_SEED_NOW)
ON DUPLICATE KEY UPDATE
    board_id = VALUES(board_id), name = VALUES(name), category = VALUES(category), parent_id = VALUES(parent_id), display_order = VALUES(display_order), is_active = TRUE, updated_at = @COMMUNITY_SEED_NOW;

INSERT INTO member_major (member_id, major_id, major_type, created_at, updated_at) VALUES
    (920001, 940004, 'PRIMARY', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 120 DAY), @COMMUNITY_SEED_NOW),
    (920002, 940008, 'PRIMARY', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 115 DAY), @COMMUNITY_SEED_NOW),
    (920003, 940006, 'PRIMARY', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 110 DAY), @COMMUNITY_SEED_NOW),
    (920004, 940010, 'PRIMARY', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 105 DAY), @COMMUNITY_SEED_NOW),
    (920005, 940007, 'PRIMARY', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 100 DAY), @COMMUNITY_SEED_NOW),
    (920006, 940005, 'PRIMARY', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 95 DAY), @COMMUNITY_SEED_NOW),
    (920007, 940012, 'PRIMARY', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 90 DAY), @COMMUNITY_SEED_NOW),
    (920008, 940007, 'PRIMARY', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 85 DAY), @COMMUNITY_SEED_NOW),
    (920009, 940009, 'PRIMARY', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 80 DAY), @COMMUNITY_SEED_NOW),
    (920010, 940011, 'PRIMARY', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 75 DAY), @COMMUNITY_SEED_NOW),
    (920011, 940012, 'PRIMARY', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 70 DAY), @COMMUNITY_SEED_NOW),
    (920012, 940005, 'PRIMARY', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 65 DAY), @COMMUNITY_SEED_NOW)
ON DUPLICATE KEY UPDATE major_type = VALUES(major_type), updated_at = @COMMUNITY_SEED_NOW;

INSERT INTO member_interests (member_id, interest_id) VALUES
    (920001, 950007), (920001, 950010), (920002, 950010), (920002, 950011),
    (920003, 950009), (920003, 950014), (920004, 950017), (920004, 950013),
    (920005, 950009), (920005, 950010), (920006, 950012), (920006, 950008),
    (920007, 950014), (920007, 950015), (920008, 950009), (920008, 950016),
    (920009, 950011), (920009, 950016), (920010, 950013), (920010, 950017),
    (920011, 950015), (920011, 950014), (920012, 950007), (920012, 950016)
ON DUPLICATE KEY UPDATE interest_id = VALUES(interest_id);

INSERT INTO `group` (
    group_id, board_id, name, description, cover_image_url, category, activity_type, region,
    visibility, status, owner_id, max_members, member_count, version, created_at, updated_at
) VALUES
    (960001, 930033, '대학연합 사이드프로젝트 랩', '기획자, 디자이너, 개발자가 8주 동안 실제 서비스를 배포합니다.', 'https://picsum.photos/seed/cluverse-project-lab/1200/700', 'PROJECT', 'HYBRID', '서울', 'PUBLIC', 'ACTIVE', 920001, 16, 7, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 100 DAY), @COMMUNITY_SEED_NOW),
    (960002, 930034, '안암 밴드클럽', '매주 합주하고 학기마다 한 번 정기 공연을 준비합니다.', 'https://picsum.photos/seed/cluverse-band/1200/700', 'CLUB', 'OFFLINE', '서울', 'PUBLIC', 'ACTIVE', 920006, 20, 12, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 95 DAY), @COMMUNITY_SEED_NOW),
    (960003, 930035, '한강 러닝 메이트', '초급과 중급 페이스로 나눠 안전하게 함께 달립니다.', 'https://picsum.photos/seed/cluverse-running/1200/700', 'SMALL_GROUP', 'OFFLINE', '서울', 'PUBLIC', 'ACTIVE', 920011, 30, 18, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 90 DAY), @COMMUNITY_SEED_NOW),
    (960004, 930036, 'AI 논문 읽기 모임', '매주 한 편을 골라 핵심 아이디어와 실험 결과를 발표합니다.', 'https://picsum.photos/seed/cluverse-ai-paper/1200/700', 'STUDY', 'ONLINE', NULL, 'PUBLIC', 'ACTIVE', 920008, 12, 9, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 85 DAY), @COMMUNITY_SEED_NOW)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), description = VALUES(description), cover_image_url = VALUES(cover_image_url), category = VALUES(category), activity_type = VALUES(activity_type), region = VALUES(region), visibility = VALUES(visibility), status = 'ACTIVE', max_members = VALUES(max_members), member_count = VALUES(member_count), updated_at = @COMMUNITY_SEED_NOW;

INSERT INTO group_interest (group_id, interest_id, created_at, updated_at) VALUES
    (960001, 950007, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 100 DAY), @COMMUNITY_SEED_NOW),
    (960001, 950008, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 100 DAY), @COMMUNITY_SEED_NOW),
    (960001, 950010, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 100 DAY), @COMMUNITY_SEED_NOW),
    (960002, 950012, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 95 DAY), @COMMUNITY_SEED_NOW),
    (960003, 950015, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 90 DAY), @COMMUNITY_SEED_NOW),
    (960004, 950009, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 85 DAY), @COMMUNITY_SEED_NOW)
ON DUPLICATE KEY UPDATE updated_at = @COMMUNITY_SEED_NOW;

INSERT INTO group_member (group_id, member_id, role, joined_at, created_at, updated_at) VALUES
    (960001, 920001, 'OWNER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 100 DAY), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 100 DAY), @COMMUNITY_SEED_NOW),
    (960001, 920002, 'ADMIN', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 82 DAY), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 82 DAY), @COMMUNITY_SEED_NOW),
    (960001, 920005, 'MEMBER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 75 DAY), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 75 DAY), @COMMUNITY_SEED_NOW),
    (960002, 920006, 'OWNER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 95 DAY), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 95 DAY), @COMMUNITY_SEED_NOW),
    (960002, 920003, 'MEMBER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 65 DAY), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 65 DAY), @COMMUNITY_SEED_NOW),
    (960003, 920011, 'OWNER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 90 DAY), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 90 DAY), @COMMUNITY_SEED_NOW),
    (960003, 920007, 'ADMIN', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 70 DAY), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 70 DAY), @COMMUNITY_SEED_NOW),
    (960004, 920008, 'OWNER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 85 DAY), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 85 DAY), @COMMUNITY_SEED_NOW),
    (960004, 920005, 'MEMBER', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 60 DAY), DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 60 DAY), @COMMUNITY_SEED_NOW)
ON DUPLICATE KEY UPDATE role = VALUES(role), updated_at = @COMMUNITY_SEED_NOW;

INSERT INTO recruitment (
    recruitment_id, group_id, author_id, title, description, positions, requirements, duration, goal,
    process_description, deadline, status, application_count, created_at, updated_at
) VALUES
    (962001, 960001, 920001, '가을 학기 사이드프로젝트 팀원 모집', '사용자 인터뷰부터 운영 배포까지 함께할 팀원을 찾습니다.', JSON_ARRAY(JSON_OBJECT('name', '백엔드', 'count', 2), JSON_OBJECT('name', '프론트엔드', 'count', 2), JSON_OBJECT('name', '디자인', 'count', 1)), '주 1회 회의와 맡은 이슈를 끝까지 진행할 수 있는 분', '8주', '실사용자 피드백을 받는 MVP 배포', '지원서 확인 후 20분 온라인 대화', DATE_ADD(@COMMUNITY_SEED_NOW, INTERVAL 14 DAY), 'OPEN', 6, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 5 DAY), @COMMUNITY_SEED_NOW),
    (962002, 960002, 920006, '정기 공연 객원 키보드 모집', '학기말 공연 두 곡을 함께할 객원 연주자를 찾습니다.', JSON_ARRAY(JSON_OBJECT('name', '키보드', 'count', 1)), '주 1회 합주와 공연 전 리허설 참여', '6주', '학기말 정기 공연', '지원 후 합주 한 번', DATE_ADD(@COMMUNITY_SEED_NOW, INTERVAL 10 DAY), 'OPEN', 3, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 4 DAY), @COMMUNITY_SEED_NOW),
    (962003, 960003, 920011, '주말 10km 페이스메이커 모집', '6분 10초 페이스 그룹을 이끌 멤버를 찾습니다.', JSON_ARRAY(JSON_OBJECT('name', '페이스메이커', 'count', 2)), '10km 완주 경험과 안전 수칙 준수', '4주', '대학연합 오픈런 준비', '기존 러닝 기록 확인', DATE_ADD(@COMMUNITY_SEED_NOW, INTERVAL 9 DAY), 'OPEN', 4, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 3 DAY), @COMMUNITY_SEED_NOW),
    (962004, 960004, 920008, '논문 발표 담당 멤버 모집', '격주로 논문 요약 발표를 맡을 멤버를 찾습니다.', JSON_ARRAY(JSON_OBJECT('name', '발표 담당', 'count', 2)), '온라인 정기 세션 참여와 10분 발표', '8주', '논문 4편 완독과 발표', '관심 분야와 발표 경험 확인', DATE_ADD(@COMMUNITY_SEED_NOW, INTERVAL 12 DAY), 'OPEN', 2, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 2 DAY), @COMMUNITY_SEED_NOW)
ON DUPLICATE KEY UPDATE
    title = VALUES(title), description = VALUES(description), positions = VALUES(positions), requirements = VALUES(requirements), duration = VALUES(duration), goal = VALUES(goal), process_description = VALUES(process_description), deadline = VALUES(deadline), status = 'OPEN', application_count = VALUES(application_count), updated_at = @COMMUNITY_SEED_NOW;

INSERT INTO post (
    post_id, board_id, member_id, title, content, category, is_anonymous, is_pinned,
    is_external_visible, status, created_at, updated_at
) VALUES
    (970001, 930001, 920001, '공학계열 2학기 전공 선택 질문 모아봅니다', '복수전공과 전과를 고민하는 분들이 자주 묻는 질문을 댓글로 모아보면 좋겠습니다.', 'QUESTION', FALSE, TRUE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 2 HOUR), @COMMUNITY_SEED_NOW),
    (970002, 930001, 920003, '캡스톤 팀 구할 때 확인하면 좋은 것들', '역할보다 먼저 주당 가능 시간과 목표 수준을 맞추는 게 중요했습니다. 깃 사용 방식도 초반에 정해두세요.', 'INFORMATION', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 1 DAY), @COMMUNITY_SEED_NOW),
    (970003, 930004, 920001, '운영체제 과목 프로젝트 주제 추천받아요', '파일 시스템이나 스케줄러를 작게 구현해 보고 싶은데 한 학기 범위에서 가능한 주제가 궁금합니다.', 'QUESTION', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 4 HOUR), @COMMUNITY_SEED_NOW),
    (970004, 930004, 920012, 'JPA N+1 정리 자료 공유합니다', '스터디에서 사용한 예제와 fetch join, entity graph 비교 내용을 정리했습니다.', 'RESOURCE', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 2 DAY), @COMMUNITY_SEED_NOW),
    (970005, 930005, 920006, '웹프로그래밍 팀플 회고', '기능을 먼저 나누기보다 공통 컴포넌트와 API 계약부터 정하니 충돌이 크게 줄었습니다.', 'REVIEW', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 3 DAY), @COMMUNITY_SEED_NOW),
    (970006, 930006, 920003, 'STM32 보드 대여 가능한 곳 있나요', '실습실 운영 시간이 끝난 뒤에도 사용할 수 있는 교내 장비 대여처를 찾고 있습니다.', 'QUESTION', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 5 HOUR), @COMMUNITY_SEED_NOW),
    (970007, 930007, 920008, '이번 학기 논문 세미나 읽기 목록', 'Transformer 해석, 평가 데이터 오염, RAG 평가 순으로 읽어보려고 합니다.', 'RESOURCE', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 8 HOUR), @COMMUNITY_SEED_NOW),
    (970008, 930002, 920002, '경상·사회계열 복수전공 설명회 후기', '학점 기준과 선수 과목, 면접 준비에 관한 질문이 가장 많았습니다. 발표 자료 링크도 함께 정리합니다.', 'REVIEW', FALSE, TRUE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 6 HOUR), @COMMUNITY_SEED_NOW),
    (970009, 930008, 920002, '마케팅원론 팀플 설문 참여 부탁드립니다', '대학생 구독 서비스 이용 경험에 관한 3분 설문입니다. 결과도 정리해서 공유하겠습니다.', 'GENERAL', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 12 HOUR), @COMMUNITY_SEED_NOW),
    (970010, 930009, 920009, '경제 데이터 분석 입문 자료 추천', '한국은행 ECOS 데이터를 처음 다뤄보려는데 수업과 함께 볼 만한 자료가 있을까요?', 'QUESTION', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 1 DAY), @COMMUNITY_SEED_NOW),
    (970011, 930010, 920004, '사용자 인터뷰 질문지 피드백 교환해요', '유도 질문을 줄이고 실제 행동을 묻는 방식으로 고쳐보고 있습니다. 서로 질문지를 바꿔서 봐요.', 'GENERAL', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 2 DAY), @COMMUNITY_SEED_NOW),
    (970012, 930003, 920010, '예술·체육계열 졸업전시 일정 모음', '학교별 공개된 졸업전시와 공연 일정을 댓글로 제보해 주세요. 본문에 계속 업데이트하겠습니다.', 'INFORMATION', FALSE, TRUE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 9 HOUR), @COMMUNITY_SEED_NOW),
    (970013, 930011, 920010, '포트폴리오 PDF 페이지 수 어느 정도가 좋을까요', 'UX 프로젝트 세 개를 담으니 분량이 길어져서 핵심 과정 중심으로 줄일지 고민입니다.', 'QUESTION', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 15 HOUR), @COMMUNITY_SEED_NOW),
    (970014, 930012, 920007, '운동처방 실습 체크리스트 공유', '심박수 측정과 설문 단계에서 놓치기 쉬운 항목을 실습 순서대로 정리했습니다.', 'RESOURCE', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 2 DAY), @COMMUNITY_SEED_NOW),
    (970015, 930013, 920009, '축제 영상 촬영 스태프 구합니다', '인터뷰 촬영과 현장 스케치를 함께할 두 분을 찾습니다. 장비는 학과에서 대여 가능합니다.', 'RECRUITMENT', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 3 DAY), @COMMUNITY_SEED_NOW),
    (970016, 930014, 920001, '이번 주 개발 밋업과 해커톤 일정 모음', '서울권 대학생이 신청할 수 있는 행사 위주로 정리했습니다. 마감된 행사는 댓글로 알려주세요.', 'INFORMATION', FALSE, TRUE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 1 HOUR), @COMMUNITY_SEED_NOW),
    (970017, 930014, 920006, '사이드 프로젝트 기술 스택 어떻게 정했나요', '익숙한 기술과 새로 배우고 싶은 기술의 비율을 어느 정도로 잡는지 궁금합니다.', 'QUESTION', TRUE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 7 HOUR), @COMMUNITY_SEED_NOW),
    (970018, 930020, 920012, 'Spring 배치 작업 실패 알림 구성 후기', '재시도 가능한 오류와 즉시 확인해야 하는 오류를 나눠 알림 채널을 구성했습니다.', 'REVIEW', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 3 HOUR), @COMMUNITY_SEED_NOW),
    (970019, 930020, 920001, '백엔드 스터디 코드리뷰 멤버 모집', '주 1회 온라인으로 PR을 함께 읽고 테스트와 쿼리 개선을 다룹니다.', 'RECRUITMENT', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 1 DAY), @COMMUNITY_SEED_NOW),
    (970020, 930021, 920010, '접근성 체크할 때 쓰는 목록 공유', '키보드 탐색, 포커스 표시, 명도 대비, 폼 레이블 순서로 확인하고 있습니다.', 'RESOURCE', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 10 HOUR), @COMMUNITY_SEED_NOW),
    (970021, 930022, 920008, 'LLM 평가 데이터셋 누수 확인 방법', '학습 데이터 포함 여부를 완전히 알 수 없을 때 적용할 수 있는 간접 점검 방법을 정리했습니다.', 'INFORMATION', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 14 HOUR), @COMMUNITY_SEED_NOW),
    (970022, 930015, 920005, '학교 안에서 첫 사용자 20명 모은 방법', '동아리 단톡방에 바로 홍보하기보다 문제 인터뷰를 먼저 요청한 게 효과가 좋았습니다.', 'REVIEW', FALSE, TRUE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 5 HOUR), @COMMUNITY_SEED_NOW),
    (970023, 930023, 920005, '아이디어 검증 인터뷰 같이 할 팀원 찾아요', '이번 주말 성수에서 인터뷰 네 건을 진행합니다. 기록과 질문을 나눠 맡을 분을 찾습니다.', 'RECRUITMENT', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 1 DAY), @COMMUNITY_SEED_NOW),
    (970024, 930024, 920009, '동아리 리브랜딩 전후 설문 문항 공유', '인지도보다 이름과 활동 내용이 얼마나 잘 연결되는지 확인하는 데 초점을 맞췄습니다.', 'RESOURCE', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 2 DAY), @COMMUNITY_SEED_NOW),
    (970025, 930016, 920006, '이번 달 대학가 공연과 전시 추천해주세요', '작은 공연장이나 학생 전시도 좋습니다. 직접 다녀온 곳이면 한 줄 후기도 부탁해요.', 'GENERAL', FALSE, TRUE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 11 HOUR), @COMMUNITY_SEED_NOW),
    (970026, 930025, 920006, '신촌 합주실 세 곳 비교 후기', '드럼 상태, 앰프, 대기 공간, 주말 가격을 기준으로 비교했습니다.', 'REVIEW', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 2 DAY), @COMMUNITY_SEED_NOW),
    (970027, 930026, 920010, '주말 크로키 번개 열어요', '처음 오시는 분도 참여할 수 있게 1분, 3분, 10분 포즈 순서로 진행합니다.', 'RECRUITMENT', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 1 DAY), @COMMUNITY_SEED_NOW),
    (970028, 930017, 920011, '이번 주말 운동 번개 모음', '풋살, 러닝, 클라이밍 일정과 남은 인원을 댓글로 업데이트해 주세요.', 'INFORMATION', FALSE, TRUE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 6 HOUR), @COMMUNITY_SEED_NOW),
    (970029, 930027, 920007, '토요일 오전 풋살 두 자리 남았습니다', '잠실 실내 구장이고 실력보다 매너와 시간 약속을 중요하게 봅니다.', 'RECRUITMENT', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 9 HOUR), @COMMUNITY_SEED_NOW),
    (970030, 930028, 920011, '여의도 8km 야간 러닝 코스', '초반 혼잡 구간과 급수 가능한 편의점 위치를 함께 정리했습니다.', 'INFORMATION', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 1 DAY), @COMMUNITY_SEED_NOW),
    (970031, 930018, 920002, '하반기 인턴 채용 일정 같이 정리해요', '직무와 마감일, 과제 여부를 기준으로 공동 문서를 만들었습니다.', 'INFORMATION', FALSE, TRUE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 4 HOUR), @COMMUNITY_SEED_NOW),
    (970032, 930029, 920009, '데이터 직무 모의면접 스터디 모집', '주 1회 SQL, 분석 사례, 지표 설계 질문으로 서로 면접을 진행합니다.', 'RECRUITMENT', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 1 DAY), @COMMUNITY_SEED_NOW),
    (970033, 930030, 920010, '개발 포트폴리오 피드백할 때 보는 기준', '문제, 선택, 결과가 연결되는지와 본인 기여가 명확한지를 우선 확인합니다.', 'INFORMATION', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 2 DAY), @COMMUNITY_SEED_NOW),
    (970034, 930019, 920004, '언어교환 모임 운영 팁 나눠주세요', '실력 차이가 클 때도 모두 말할 수 있도록 주제와 시간을 나누는 방법이 궁금합니다.', 'QUESTION', FALSE, TRUE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 13 HOUR), @COMMUNITY_SEED_NOW),
    (970035, 930031, 920002, '목요일 저녁 영어 회화 네 명 모집', '신촌 카페에서 90분 동안 일상과 커리어 주제로 이야기합니다.', 'RECRUITMENT', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 1 DAY), @COMMUNITY_SEED_NOW),
    (970036, 930032, 920009, '일본 교환학생 준비 표현 정리', '기숙사, 수강 신청, 동아리 가입 때 자주 쓰는 표현을 상황별로 정리했습니다.', 'RESOURCE', FALSE, FALSE, TRUE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 3 DAY), @COMMUNITY_SEED_NOW),
    (970037, 930033, 920001, '이번 주 스프린트 목표와 역할 공유', '인터뷰 결과 반영, 온보딩 수정, 배포 자동화까지 세 트랙으로 진행합니다.', 'NOTICE', FALSE, TRUE, FALSE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 8 HOUR), @COMMUNITY_SEED_NOW),
    (970038, 930034, 920006, '정기 공연 셋리스트 후보 받아요', '보컬 키와 합주 난이도를 함께 적어주세요. 이번 주 합주에서 세 곡으로 줄이겠습니다.', 'GENERAL', FALSE, TRUE, FALSE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 1 DAY), @COMMUNITY_SEED_NOW),
    (970039, 930035, 920011, '비 오는 날 대체 코스 안내', '우천 시에는 실내 트랙으로 변경하고 출발 시간은 그대로 유지합니다.', 'NOTICE', FALSE, TRUE, FALSE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 2 DAY), @COMMUNITY_SEED_NOW),
    (970040, 930036, 920008, '다음 세션은 RAG 평가 논문입니다', '발표 자료는 열 장 이내로 준비하고 재현이 어려운 부분을 질문으로 남겨주세요.', 'NOTICE', FALSE, TRUE, FALSE, 'ACTIVE', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 3 DAY), @COMMUNITY_SEED_NOW)
ON DUPLICATE KEY UPDATE
    board_id = VALUES(board_id), member_id = VALUES(member_id), title = VALUES(title), content = VALUES(content), category = VALUES(category), is_anonymous = VALUES(is_anonymous), is_pinned = VALUES(is_pinned), is_external_visible = VALUES(is_external_visible), status = 'ACTIVE', deleted_at = NULL, updated_at = @COMMUNITY_SEED_NOW;

INSERT INTO post_view_count (post_id, view_count, created_at, updated_at)
SELECT post_id, 40 + MOD(post_id * 17, 190), created_at, @COMMUNITY_SEED_NOW
FROM post WHERE post_id BETWEEN 970001 AND 970040
ON DUPLICATE KEY UPDATE view_count = VALUES(view_count), updated_at = @COMMUNITY_SEED_NOW;

INSERT INTO post_like_count (post_id, like_count, created_at, updated_at)
SELECT post_id, 2 + MOD(post_id * 7, 24), created_at, @COMMUNITY_SEED_NOW
FROM post WHERE post_id BETWEEN 970001 AND 970040
ON DUPLICATE KEY UPDATE like_count = VALUES(like_count), updated_at = @COMMUNITY_SEED_NOW;

INSERT INTO post_bookmark_count (post_id, bookmark_count, created_at, updated_at)
SELECT post_id, MOD(post_id * 5, 12), created_at, @COMMUNITY_SEED_NOW
FROM post WHERE post_id BETWEEN 970001 AND 970040
ON DUPLICATE KEY UPDATE bookmark_count = VALUES(bookmark_count), updated_at = @COMMUNITY_SEED_NOW;

INSERT INTO post_tag (post_id, tag_name) VALUES
    (970001, '전공선택'), (970002, '캡스톤'), (970003, '운영체제'), (970004, 'spring'),
    (970007, '논문'), (970008, '복수전공'), (970011, '사용자인터뷰'), (970012, '졸업전시'),
    (970016, '개발행사'), (970018, 'spring'), (970019, '스터디'), (970020, '접근성'),
    (970021, 'LLM'), (970022, '아이디어검증'), (970026, '밴드'), (970028, '운동번개'),
    (970031, '인턴'), (970033, '포트폴리오'), (970034, '언어교환'), (970037, '사이드프로젝트')
ON DUPLICATE KEY UPDATE tag_name = VALUES(tag_name);

INSERT INTO comment (
    comment_id, post_id, member_id, parent_id, depth, path, content, is_anonymous, status,
    like_count, reply_count, created_at, updated_at
) VALUES
    (980001, 970001, 920003, NULL, 0, '20260825100000-00000000000000980001', '전자전기는 회로 실습 과목을 먼저 들어보면 결정에 도움이 됐어요.', FALSE, 'ACTIVE', 4, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 90 MINUTE), @COMMUNITY_SEED_NOW),
    (980002, 970003, 920012, NULL, 0, '20260825110000-00000000000000980002', '스케줄러 시각화 도구를 붙이면 시연할 때 반응이 좋았습니다.', FALSE, 'ACTIVE', 6, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 2 HOUR), @COMMUNITY_SEED_NOW),
    (980003, 970006, 920008, NULL, 0, '20260825120000-00000000000000980003', '메이커스페이스에서 수업 외 대여도 받고 있어요.', FALSE, 'ACTIVE', 3, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 3 HOUR), @COMMUNITY_SEED_NOW),
    (980004, 970009, 920005, NULL, 0, '20260825130000-00000000000000980004', '참여했습니다. 결과 공유도 기대할게요!', FALSE, 'ACTIVE', 2, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 4 HOUR), @COMMUNITY_SEED_NOW),
    (980005, 970013, 920002, NULL, 0, '20260825140000-00000000000000980005', '첫 화면에서 프로젝트별 핵심 결과가 보이면 페이지 수가 조금 길어도 괜찮았어요.', FALSE, 'ACTIVE', 5, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 5 HOUR), @COMMUNITY_SEED_NOW),
    (980006, 970016, 920008, NULL, 0, '20260825150000-00000000000000980006', 'AI 해커톤 한 곳 더 제보합니다. 신청 마감은 다음 주 월요일이에요.', FALSE, 'ACTIVE', 8, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 30 MINUTE), @COMMUNITY_SEED_NOW),
    (980007, 970017, 920001, NULL, 0, '20260825160000-00000000000000980007', '핵심 기능은 익숙한 기술로 만들고 한 영역만 새 기술을 쓰는 편입니다.', FALSE, 'ACTIVE', 7, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 3 HOUR), @COMMUNITY_SEED_NOW),
    (980008, 970019, 920010, NULL, 0, '20260825170000-00000000000000980008', '프론트 개발자도 API 설계 리뷰에 참여할 수 있을까요?', FALSE, 'ACTIVE', 3, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 8 HOUR), @COMMUNITY_SEED_NOW),
    (980009, 970022, 920002, NULL, 0, '20260825180000-00000000000000980009', '인터뷰 요청 문구를 짧게 쓴 것도 응답률에 도움이 됐나요?', FALSE, 'ACTIVE', 4, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 2 HOUR), @COMMUNITY_SEED_NOW),
    (980010, 970025, 920010, NULL, 0, '20260825190000-00000000000000980010', '홍대 앞 독립 전시 하나 추천합니다. 주말까지 무료예요.', FALSE, 'ACTIVE', 6, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 6 HOUR), @COMMUNITY_SEED_NOW),
    (980011, 970028, 920007, NULL, 0, '20260825200000-00000000000000980011', '풋살은 토요일 오전 두 자리 남았습니다.', FALSE, 'ACTIVE', 5, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 1 HOUR), @COMMUNITY_SEED_NOW),
    (980012, 970031, 920009, NULL, 0, '20260825210000-00000000000000980012', '마케팅 직무 일정도 문서에 추가해두었습니다.', FALSE, 'ACTIVE', 2, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 2 HOUR), @COMMUNITY_SEED_NOW),
    (980013, 970034, 920006, NULL, 0, '20260825220000-00000000000000980013', '두 명씩 먼저 이야기하고 전체 공유로 넘어가면 발화량이 고르게 나왔어요.', FALSE, 'ACTIVE', 7, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 4 HOUR), @COMMUNITY_SEED_NOW),
    (980014, 970037, 920002, NULL, 0, '20260825230000-00000000000000980014', '인터뷰 결과 정리는 오늘 밤까지 올려둘게요.', FALSE, 'ACTIVE', 3, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 40 MINUTE), @COMMUNITY_SEED_NOW),
    (980015, 970040, 920005, NULL, 0, '20260826000000-00000000000000980015', '평가 데이터셋 파트 재현 자료를 찾아보겠습니다.', FALSE, 'ACTIVE', 4, 0, DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 1 DAY), @COMMUNITY_SEED_NOW)
ON DUPLICATE KEY UPDATE
    post_id = VALUES(post_id), member_id = VALUES(member_id), content = VALUES(content), status = 'ACTIVE', like_count = VALUES(like_count), reply_count = VALUES(reply_count), deleted_at = NULL, updated_at = @COMMUNITY_SEED_NOW;

INSERT INTO post_comment_count (post_id, comment_count, created_at, updated_at)
SELECT p.post_id, COUNT(c.comment_id), p.created_at, @COMMUNITY_SEED_NOW
FROM post p LEFT JOIN comment c ON c.post_id = p.post_id AND c.status = 'ACTIVE'
WHERE p.post_id BETWEEN 970001 AND 970040
GROUP BY p.post_id, p.created_at
ON DUPLICATE KEY UPDATE comment_count = VALUES(comment_count), updated_at = @COMMUNITY_SEED_NOW;

INSERT INTO post_comment_activity (post_id, last_comment_id, last_commented_at, created_at, updated_at)
SELECT c.post_id, c.comment_id, c.created_at, c.created_at, @COMMUNITY_SEED_NOW
FROM comment c
JOIN (
    SELECT post_id, MAX(comment_id) AS last_comment_id
    FROM comment
    WHERE comment_id BETWEEN 980001 AND 980015 AND status = 'ACTIVE'
    GROUP BY post_id
) latest ON latest.last_comment_id = c.comment_id
ON DUPLICATE KEY UPDATE
    last_comment_id = VALUES(last_comment_id), last_commented_at = VALUES(last_commented_at), updated_at = @COMMUNITY_SEED_NOW;

INSERT INTO campus_event (
    campus_event_id, title, host, start_date, end_date, location, thumbnail_image_url, summary, created_at, updated_at
) VALUES
    (993001, '서울권 대학연합 해커톤 오리엔테이션', '클루버스 대학연합 운영진', DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), '서울대학교 해동학술문화관', 'https://picsum.photos/seed/cluverse-event-hackathon/1200/800', '개발, 기획, 디자인 참가자가 현장에서 팀을 만들고 트랙별 일정을 안내받습니다.', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 5 DAY), @COMMUNITY_SEED_NOW),
    (993002, '대학생 프로덕트 피칭 나이트', '연세대학교 창업지원단', DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY), '연세대학교 백양누리', 'https://picsum.photos/seed/cluverse-event-pitch/1200/800', '초기 아이디어를 5분 동안 발표하고 현직자와 학생 참가자의 피드백을 받습니다.', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 4 DAY), @COMMUNITY_SEED_NOW),
    (993003, '가을 학기 동아리 박람회', '서울권 6개 대학 총동아리연합회', DATE_ADD(CURRENT_DATE, INTERVAL 9 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), '신촌 대학가 연합 부스', 'https://picsum.photos/seed/cluverse-event-clubfair/1200/800', '개발, 공연, 스포츠, 봉사 분야 동아리가 활동과 모집 일정을 소개합니다.', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 3 DAY), @COMMUNITY_SEED_NOW),
    (993004, '대학연합 한강 오픈런', '한강 러닝 메이트', DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), '여의도 한강공원 이벤트 광장', 'https://picsum.photos/seed/cluverse-event-running/1200/800', '초급 5km와 중급 10km 그룹으로 나눠 달리는 대학생 오픈런입니다.', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 2 DAY), @COMMUNITY_SEED_NOW),
    (993005, '학생 창작자 미디어 아트 전시', '홍익대학교 학생기획단', DATE_ADD(CURRENT_DATE, INTERVAL 15 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 18 DAY), '홍익대학교 문헌관 현대미술관', 'https://picsum.photos/seed/cluverse-event-mediaart/1200/800', '영상, 인터랙션, 그래픽 작업을 전시하고 저녁에는 참여 작가 토크가 열립니다.', DATE_SUB(@COMMUNITY_SEED_NOW, INTERVAL 1 DAY), @COMMUNITY_SEED_NOW)
ON DUPLICATE KEY UPDATE
    title = VALUES(title), host = VALUES(host), start_date = VALUES(start_date), end_date = VALUES(end_date), location = VALUES(location), thumbnail_image_url = VALUES(thumbnail_image_url), summary = VALUES(summary), updated_at = @COMMUNITY_SEED_NOW;
