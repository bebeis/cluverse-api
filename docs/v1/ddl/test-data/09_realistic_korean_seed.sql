-- ============================================================
-- Realistic Korean service seed
-- 목적:
--   - 실제 한국 대학/학과/관심사/소모임이 운영 중인 서비스처럼 보이는 샘플 데이터
-- 주의:
--   - 학교명과 학교 메일 도메인은 실제 정보를 사용합니다.
--   - 회원, 모임, 게시글, 지원서는 모두 가상의 샘플 데이터입니다.
--   - 같은 파일을 재실행해도 삽입 대상 테이블을 모두 비우고 다시 넣도록 구성했습니다.
-- ============================================================

SET @SEED_NOW = '2026-03-17 20:00:00';

SET @UNIVERSITY_START_ID = 91001;
SET @UNIVERSITY_END_ID = 91012;
SET @TERMS_START_ID = 91101;
SET @TERMS_END_ID = 91103;
SET @MEMBER_START_ID = 920001;
SET @MEMBER_END_ID = 920024;
SET @BOARD_START_ID = 930001;
SET @BOARD_END_ID = 930040;
SET @MAJOR_START_ID = 940001;
SET @MAJOR_END_ID = 940013;
SET @INTEREST_START_ID = 950001;
SET @INTEREST_END_ID = 950019;
SET @GROUP_START_ID = 960001;
SET @GROUP_END_ID = 960008;
SET @ROLE_START_ID = 961001;
SET @ROLE_END_ID = 961008;
SET @RECRUITMENT_START_ID = 962001;
SET @RECRUITMENT_END_ID = 962003;
SET @FORM_ITEM_START_ID = 963001;
SET @FORM_ITEM_END_ID = 963009;
SET @APPLICATION_START_ID = 964001;
SET @APPLICATION_END_ID = 964006;
SET @POST_START_ID = 970001;
SET @POST_END_ID = 970018;
SET @COMMENT_START_ID = 980001;
SET @COMMENT_END_ID = 980021;
SET @FRONT_NOTIFICATION_START_ID = 990001;
SET @FRONT_NOTIFICATION_END_ID = 990008;
SET @FRONT_REPORT_START_ID = 991001;
SET @FRONT_REPORT_END_ID = 991003;
SET @FRONT_CALENDAR_EVENT_START_ID = 992001;
SET @FRONT_CALENDAR_EVENT_END_ID = 992008;
SET @FRONT_CAMPUS_EVENT_START_ID = 993001;
SET @FRONT_CAMPUS_EVENT_END_ID = 993004;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE application_chat_message;
TRUNCATE TABLE form_item_answer;
TRUNCATE TABLE application_status_history;
TRUNCATE TABLE recruitment_application;
TRUNCATE TABLE form_item;
TRUNCATE TABLE recruitment;
TRUNCATE TABLE calendar_event;
TRUNCATE TABLE campus_event;
TRUNCATE TABLE calendar_item;
TRUNCATE TABLE member_report_evidence_image;
TRUNCATE TABLE member_report;
TRUNCATE TABLE group_member_history;
TRUNCATE TABLE group_member;
TRUNCATE TABLE group_interest;
TRUNCATE TABLE group_role;
TRUNCATE TABLE `group`;
TRUNCATE TABLE follow;
TRUNCATE TABLE comment_like;
TRUNCATE TABLE comment;
TRUNCATE TABLE post_like;
TRUNCATE TABLE bookmark;
TRUNCATE TABLE post_tag;
TRUNCATE TABLE post_image;
TRUNCATE TABLE post_view_count;
TRUNCATE TABLE post_like_count;
TRUNCATE TABLE post_comment_count;
TRUNCATE TABLE post_bookmark_count;
TRUNCATE TABLE post;
TRUNCATE TABLE member_interests;
TRUNCATE TABLE interest_major_relation;
TRUNCATE TABLE interest;
TRUNCATE TABLE member_major;
TRUNCATE TABLE major;
TRUNCATE TABLE board;
TRUNCATE TABLE notification_preference;
TRUNCATE TABLE notification;
TRUNCATE TABLE notification_setting;
TRUNCATE TABLE member_terms_agreement;
TRUNCATE TABLE student_verification_email_challenge;
TRUNCATE TABLE student_verification;
TRUNCATE TABLE social_account;
TRUNCATE TABLE member_status_history;
TRUNCATE TABLE member_profile;
TRUNCATE TABLE member_auth;
TRUNCATE TABLE member;
TRUNCATE TABLE terms;
TRUNCATE TABLE university;

SET FOREIGN_KEY_CHECKS = 1;

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
    (91001, '서울대학교', 'snu.ac.kr', 'https://picsum.photos/seed/univ-snu/200/200', '서울특별시 관악구 관악로 1', TRUE, DATE_SUB(@SEED_NOW, INTERVAL 180 DAY), @SEED_NOW),
    (91002, '고려대학교', 'korea.ac.kr', 'https://picsum.photos/seed/univ-korea/200/200', '서울특별시 성북구 안암로 145', TRUE, DATE_SUB(@SEED_NOW, INTERVAL 180 DAY), @SEED_NOW),
    (91003, '연세대학교', 'yonsei.ac.kr', 'https://picsum.photos/seed/univ-yonsei/200/200', '서울특별시 서대문구 연세로 50', TRUE, DATE_SUB(@SEED_NOW, INTERVAL 180 DAY), @SEED_NOW),
    (91004, '성균관대학교', 'skku.edu', 'https://picsum.photos/seed/univ-skku/200/200', '서울특별시 종로구 성균관로 25-2', TRUE, DATE_SUB(@SEED_NOW, INTERVAL 180 DAY), @SEED_NOW),
    (91005, '한양대학교', 'hanyang.ac.kr', 'https://picsum.photos/seed/univ-hanyang/200/200', '서울특별시 성동구 왕십리로 222', TRUE, DATE_SUB(@SEED_NOW, INTERVAL 180 DAY), @SEED_NOW),
    (91006, '경희대학교', 'khu.ac.kr', 'https://picsum.photos/seed/univ-khu/200/200', '서울특별시 동대문구 경희대로 26', TRUE, DATE_SUB(@SEED_NOW, INTERVAL 180 DAY), @SEED_NOW),
    (91007, '중앙대학교', 'cau.ac.kr', 'https://picsum.photos/seed/univ-cau/200/200', '서울특별시 동작구 흑석로 84', TRUE, DATE_SUB(@SEED_NOW, INTERVAL 180 DAY), @SEED_NOW),
    (91008, '서강대학교', 'sogang.ac.kr', 'https://picsum.photos/seed/univ-sogang/200/200', '서울특별시 마포구 백범로 35', TRUE, DATE_SUB(@SEED_NOW, INTERVAL 180 DAY), @SEED_NOW),
    (91009, '부산대학교', 'pusan.ac.kr', 'https://picsum.photos/seed/univ-pusan/200/200', '부산광역시 금정구 부산대학로63번길 2', TRUE, DATE_SUB(@SEED_NOW, INTERVAL 180 DAY), @SEED_NOW),
    (91010, 'KAIST', 'kaist.ac.kr', 'https://picsum.photos/seed/univ-kaist/200/200', '대전광역시 유성구 대학로 291', TRUE, DATE_SUB(@SEED_NOW, INTERVAL 180 DAY), @SEED_NOW),
    (91011, 'POSTECH', 'postech.ac.kr', 'https://picsum.photos/seed/univ-postech/200/200', '경상북도 포항시 남구 청암로 77', TRUE, DATE_SUB(@SEED_NOW, INTERVAL 180 DAY), @SEED_NOW),
    (91012, '홍익대학교', 'hongik.ac.kr', 'https://picsum.photos/seed/univ-hongik/200/200', '서울특별시 마포구 와우산로 94', TRUE, DATE_SUB(@SEED_NOW, INTERVAL 180 DAY), @SEED_NOW);

INSERT INTO terms (
    terms_id,
    terms_type,
    title,
    content,
    version,
    is_required,
    is_active,
    effective_at,
    created_at,
    updated_at
) VALUES
    (91101, 'SERVICE', '클루버스 서비스 이용약관', '캠퍼스 커뮤니티 운영을 위한 서비스 이용약관 샘플 데이터입니다.', '2026.03-demo', TRUE, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 60 DAY), DATE_SUB(@SEED_NOW, INTERVAL 60 DAY), @SEED_NOW),
    (91102, 'PRIVACY', '클루버스 개인정보 처리방침', '회원 인증과 커뮤니티 운영을 위한 개인정보 처리방침 샘플 데이터입니다.', '2026.03-demo', TRUE, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 60 DAY), DATE_SUB(@SEED_NOW, INTERVAL 60 DAY), @SEED_NOW),
    (91103, 'MARKETING', '클루버스 마케팅 정보 수신 동의', '이벤트와 모집 소식을 알리기 위한 선택 동의 샘플 데이터입니다.', '2026.03-demo', FALSE, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 60 DAY), DATE_SUB(@SEED_NOW, INTERVAL 60 DAY), @SEED_NOW);

INSERT INTO member (
    member_id,
    nickname,
    university_id,
    status,
    verification_status,
    verification_rejected_reason,
    role,
    last_login_at,
    source_system,
    client_ip,
    created_at,
    updated_at
) VALUES
    (920001, '민준.dev', 91001, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 2 HOUR), 'MOBILE_APP', '203.0.113.11', DATE_SUB(@SEED_NOW, INTERVAL 140 DAY), @SEED_NOW),
    (920002, '서연PM', 91003, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 6 HOUR), 'WEB_USER', '203.0.113.12', DATE_SUB(@SEED_NOW, INTERVAL 132 DAY), @SEED_NOW),
    (920003, '도현회로', 91002, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 1 DAY), 'MOBILE_APP', '203.0.113.13', DATE_SUB(@SEED_NOW, INTERVAL 128 DAY), @SEED_NOW),
    (920004, '하은심리', 91007, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 12 HOUR), 'WEB_USER', '203.0.113.14', DATE_SUB(@SEED_NOW, INTERVAL 121 DAY), @SEED_NOW),
    (920005, '지우창업', 91010, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 4 HOUR), 'MOBILE_APP', '203.0.113.15', DATE_SUB(@SEED_NOW, INTERVAL 119 DAY), @SEED_NOW),
    (920006, '예준밴드', 91005, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 10 HOUR), 'MOBILE_APP', '203.0.113.16', DATE_SUB(@SEED_NOW, INTERVAL 116 DAY), @SEED_NOW),
    (920007, '윤서풋살', 91006, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 5 HOUR), 'MOBILE_APP', '203.0.113.17', DATE_SUB(@SEED_NOW, INTERVAL 112 DAY), @SEED_NOW),
    (920008, '현우데이터', 91011, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 8 HOUR), 'WEB_USER', '203.0.113.18', DATE_SUB(@SEED_NOW, INTERVAL 108 DAY), @SEED_NOW),
    (920009, '채원마케터', 91004, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 9 HOUR), 'MOBILE_APP', '203.0.113.19', DATE_SUB(@SEED_NOW, INTERVAL 104 DAY), @SEED_NOW),
    (920010, '수아UX', 91012, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 3 HOUR), 'WEB_USER', '203.0.113.20', DATE_SUB(@SEED_NOW, INTERVAL 99 DAY), @SEED_NOW),
    (920011, '태윤러너', 91008, 'ACTIVE', 'NONE', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 11 HOUR), 'MOBILE_APP', '203.0.113.21', DATE_SUB(@SEED_NOW, INTERVAL 95 DAY), @SEED_NOW),
    (920012, '민지아트', 91012, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 7 HOUR), 'MOBILE_APP', '203.0.113.22', DATE_SUB(@SEED_NOW, INTERVAL 92 DAY), @SEED_NOW),
    (920013, '가을백엔드', 91009, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 1 HOUR), 'WEB_USER', '203.0.113.23', DATE_SUB(@SEED_NOW, INTERVAL 88 DAY), @SEED_NOW),
    (920014, '준호프론트', 91001, 'ACTIVE', 'APPROVED', NULL, 'MODERATOR', DATE_SUB(@SEED_NOW, INTERVAL 30 MINUTE), 'WEB_USER', '203.0.113.24', DATE_SUB(@SEED_NOW, INTERVAL 84 DAY), @SEED_NOW),
    (920015, '유나브랜딩', 91002, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 13 HOUR), 'MOBILE_APP', '203.0.113.25', DATE_SUB(@SEED_NOW, INTERVAL 80 DAY), @SEED_NOW),
    (920016, '시우AI', 91003, 'ACTIVE', 'PENDING', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), 'WEB_USER', '203.0.113.26', DATE_SUB(@SEED_NOW, INTERVAL 75 DAY), @SEED_NOW),
    (920017, '나래독서', 91004, 'ACTIVE', 'NONE', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 1 DAY), 'MOBILE_APP', '203.0.113.27', DATE_SUB(@SEED_NOW, INTERVAL 71 DAY), @SEED_NOW),
    (920018, '재현드럼', 91005, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 16 HOUR), 'MOBILE_APP', '203.0.113.28', DATE_SUB(@SEED_NOW, INTERVAL 69 DAY), @SEED_NOW),
    (920019, '다인축구', 91006, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 6 HOUR), 'MOBILE_APP', '203.0.113.29', DATE_SUB(@SEED_NOW, INTERVAL 66 DAY), @SEED_NOW),
    (920020, '로운경제', 91007, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 14 HOUR), 'WEB_USER', '203.0.113.30', DATE_SUB(@SEED_NOW, INTERVAL 62 DAY), @SEED_NOW),
    (920021, '보라영어', 91008, 'ACTIVE', 'NONE', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 20 HOUR), 'MOBILE_APP', '203.0.113.31', DATE_SUB(@SEED_NOW, INTERVAL 58 DAY), @SEED_NOW),
    (920022, '한결영상', 91012, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 18 HOUR), 'WEB_USER', '203.0.113.32', DATE_SUB(@SEED_NOW, INTERVAL 54 DAY), @SEED_NOW),
    (920023, '지호로봇', 91010, 'ACTIVE', 'APPROVED', NULL, 'MEMBER', DATE_SUB(@SEED_NOW, INTERVAL 5 HOUR), 'MOBILE_APP', '203.0.113.33', DATE_SUB(@SEED_NOW, INTERVAL 50 DAY), @SEED_NOW),
    (920024, '클루관리자', 91001, 'ACTIVE', 'APPROVED', NULL, 'ADMIN', DATE_SUB(@SEED_NOW, INTERVAL 15 MINUTE), 'WEB_ADMIN', '203.0.113.34', DATE_SUB(@SEED_NOW, INTERVAL 200 DAY), @SEED_NOW);

INSERT INTO member_auth (
    member_id,
    email,
    password_hash,
    created_at,
    updated_at
) VALUES
    (920001, 'minjun.dev@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 140 DAY), @SEED_NOW),
    (920002, 'seoyeon.pm@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 132 DAY), @SEED_NOW),
    (920003, 'dohyeon.circuit@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 128 DAY), @SEED_NOW),
    (920004, 'haeun.mind@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 121 DAY), @SEED_NOW),
    (920005, 'jiwoo.startup@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 119 DAY), @SEED_NOW),
    (920006, 'yejun.band@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 116 DAY), @SEED_NOW),
    (920007, 'yunseo.futsal@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 112 DAY), @SEED_NOW),
    (920008, 'hyunwoo.data@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 108 DAY), @SEED_NOW),
    (920009, 'chaewon.brand@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 104 DAY), @SEED_NOW),
    (920010, 'sua.ux@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 99 DAY), @SEED_NOW),
    (920011, 'taeyun.run@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 95 DAY), @SEED_NOW),
    (920012, 'minji.art@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 92 DAY), @SEED_NOW),
    (920013, 'gaeul.backend@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 88 DAY), @SEED_NOW),
    (920014, 'junho.front@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 84 DAY), @SEED_NOW),
    (920015, 'yuna.branding@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 80 DAY), @SEED_NOW),
    (920016, 'siwoo.ai@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 75 DAY), @SEED_NOW),
    (920017, 'narae.read@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 71 DAY), @SEED_NOW),
    (920018, 'jaehyun.drum@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 69 DAY), @SEED_NOW),
    (920019, 'dain.ball@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 66 DAY), @SEED_NOW),
    (920020, 'rowoon.econ@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 62 DAY), @SEED_NOW),
    (920021, 'bora.english@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 58 DAY), @SEED_NOW),
    (920022, 'hangyeol.media@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 54 DAY), @SEED_NOW),
    (920023, 'jiho.robot@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 50 DAY), @SEED_NOW),
    (920024, 'admin@cluverse-demo.kr', '$2a$10$8l6GdC7HYSYQwNSKGwstwedK3vmbqP1O/29FLpZg2fZEGf1/WBubu', DATE_SUB(@SEED_NOW, INTERVAL 200 DAY), @SEED_NOW);

INSERT INTO member_profile (
    member_id,
    bio,
    entrance_year,
    profile_image_url,
    link_github,
    link_notion,
    link_portfolio,
    link_instagram,
    link_etc,
    is_public,
    visible_fields,
    created_at,
    updated_at
) VALUES
    (920001, '서울대 컴공. Spring와 모임 운영에 관심이 많고 해커톤 팀빌딩을 자주 합니다.', 2021, 'https://i.pravatar.cc/150?u=920001', 'https://github.com/minjun-dev-demo', 'https://www.notion.so/cluverse/minjun-dev', 'https://picsum.photos/seed/portfolio-minjun/800/600', NULL, NULL, TRUE, JSON_OBJECT('bio', TRUE, 'github', TRUE, 'portfolio', TRUE, 'instagram', FALSE), DATE_SUB(@SEED_NOW, INTERVAL 140 DAY), @SEED_NOW),
    (920002, '연세대 경영. PM 세션과 사이드프로젝트를 자주 열고 운영합니다.', 2021, 'https://i.pravatar.cc/150?u=920002', NULL, 'https://www.notion.so/cluverse/seoyeon-pm', NULL, 'https://instagram.com/seoyeon.pm.demo', NULL, TRUE, JSON_OBJECT('bio', TRUE, 'github', FALSE, 'portfolio', FALSE, 'instagram', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 132 DAY), @SEED_NOW),
    (920003, '고려대 전자전기. 임베디드와 회로 설계를 좋아합니다.', 2020, 'https://i.pravatar.cc/150?u=920003', 'https://github.com/dohyeon-circuit-demo', NULL, NULL, NULL, NULL, TRUE, JSON_OBJECT('bio', TRUE, 'github', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 128 DAY), @SEED_NOW),
    (920004, '중앙대 심리. 사용자 리서치와 인터뷰 정리에 강점이 있습니다.', 2022, 'https://i.pravatar.cc/150?u=920004', NULL, 'https://www.notion.so/cluverse/haeun-mind', NULL, NULL, NULL, TRUE, JSON_OBJECT('bio', TRUE, 'notion', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 121 DAY), @SEED_NOW),
    (920005, 'KAIST AI. 창업 아이디어 검증과 데이터 기반 제품 설계를 좋아합니다.', 2021, 'https://i.pravatar.cc/150?u=920005', 'https://github.com/jiwoo-startup-demo', 'https://www.notion.so/cluverse/jiwoo-startup', NULL, NULL, NULL, TRUE, JSON_OBJECT('bio', TRUE, 'github', TRUE, 'notion', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 119 DAY), @SEED_NOW),
    (920006, '한양대 소프트웨어. 밴드 보컬과 웹 개발을 같이 하고 있습니다.', 2020, 'https://i.pravatar.cc/150?u=920006', 'https://github.com/yejun-band-demo', NULL, NULL, 'https://instagram.com/yejun.band.demo', NULL, TRUE, JSON_OBJECT('bio', TRUE, 'github', TRUE, 'instagram', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 116 DAY), @SEED_NOW),
    (920007, '경희대 스포츠과학. 풋살 번개와 교내 리그 정보에 빠릅니다.', 2022, 'https://i.pravatar.cc/150?u=920007', NULL, NULL, NULL, 'https://instagram.com/yunseo.futsal.demo', NULL, TRUE, JSON_OBJECT('bio', TRUE, 'instagram', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 112 DAY), @SEED_NOW),
    (920008, 'POSTECH AI. 논문 읽기 모임과 데이터 파이프라인에 관심이 큽니다.', 2021, 'https://i.pravatar.cc/150?u=920008', 'https://github.com/hyunwoo-data-demo', 'https://www.notion.so/cluverse/hyunwoo-data', NULL, NULL, NULL, TRUE, JSON_OBJECT('bio', TRUE, 'github', TRUE, 'notion', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 108 DAY), @SEED_NOW),
    (920009, '성균관대 경제. 마케팅과 포지셔닝 정리를 자주 도와줍니다.', 2021, 'https://i.pravatar.cc/150?u=920009', NULL, 'https://www.notion.so/cluverse/chaewon-brand', NULL, NULL, NULL, TRUE, JSON_OBJECT('bio', TRUE, 'notion', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 104 DAY), @SEED_NOW),
    (920010, '홍대 디자인. UX와 브랜딩, 포트폴리오 피드백을 자주 합니다.', 2022, 'https://i.pravatar.cc/150?u=920010', NULL, NULL, 'https://picsum.photos/seed/portfolio-sua/800/600', 'https://instagram.com/sua.ux.demo', NULL, TRUE, JSON_OBJECT('bio', TRUE, 'portfolio', TRUE, 'instagram', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 99 DAY), @SEED_NOW),
    (920011, '서강대 경제. 러닝 크루 운영하면서 주말 번개를 자주 엽니다.', 2023, 'https://i.pravatar.cc/150?u=920011', NULL, NULL, NULL, 'https://instagram.com/taeyun.run.demo', NULL, TRUE, JSON_OBJECT('bio', TRUE, 'instagram', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 95 DAY), @SEED_NOW),
    (920012, '홍대 미디어. 드로잉과 전시 후기 글을 자주 올립니다.', 2022, 'https://i.pravatar.cc/150?u=920012', NULL, 'https://www.notion.so/cluverse/minji-art', 'https://picsum.photos/seed/portfolio-minji/800/600', 'https://instagram.com/minji.art.demo', NULL, TRUE, JSON_OBJECT('bio', TRUE, 'portfolio', TRUE, 'instagram', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 92 DAY), @SEED_NOW),
    (920013, '부산대 소프트웨어. 백엔드 스터디 정리 글을 꾸준히 올립니다.', 2020, 'https://i.pravatar.cc/150?u=920013', 'https://github.com/gaeul-backend-demo', NULL, NULL, NULL, NULL, TRUE, JSON_OBJECT('bio', TRUE, 'github', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 88 DAY), @SEED_NOW),
    (920014, '서울대 컴공. 프론트엔드와 커뮤니티 운영 모더레이션을 맡고 있습니다.', 2020, 'https://i.pravatar.cc/150?u=920014', 'https://github.com/junho-front-demo', 'https://www.notion.so/cluverse/junho-front', 'https://picsum.photos/seed/portfolio-junho/800/600', NULL, NULL, TRUE, JSON_OBJECT('bio', TRUE, 'github', TRUE, 'portfolio', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 84 DAY), @SEED_NOW),
    (920015, '고려대 경영. 브랜딩, 카피라이팅, 행사 포스터 작업을 즐깁니다.', 2021, 'https://i.pravatar.cc/150?u=920015', NULL, NULL, 'https://picsum.photos/seed/portfolio-yuna/800/600', 'https://instagram.com/yuna.branding.demo', NULL, TRUE, JSON_OBJECT('bio', TRUE, 'portfolio', TRUE, 'instagram', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 80 DAY), @SEED_NOW),
    (920016, '연세대 AI. 모델 실험 기록을 잘 남기고, 프론트에도 관심이 있습니다.', 2023, 'https://i.pravatar.cc/150?u=920016', 'https://github.com/siwoo-ai-demo', 'https://www.notion.so/cluverse/siwoo-ai', NULL, NULL, NULL, TRUE, JSON_OBJECT('bio', TRUE, 'github', TRUE, 'notion', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 75 DAY), @SEED_NOW),
    (920017, '성균관대 심리. 독서모임과 진로 관련 글을 즐겨 읽습니다.', 2024, 'https://i.pravatar.cc/150?u=920017', NULL, NULL, NULL, NULL, NULL, TRUE, JSON_OBJECT('bio', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 71 DAY), @SEED_NOW),
    (920018, '한양대 전자전기. 드럼 연주와 오디오 장비에 관심이 많습니다.', 2021, 'https://i.pravatar.cc/150?u=920018', NULL, NULL, NULL, 'https://instagram.com/jaehyun.drum.demo', NULL, TRUE, JSON_OBJECT('bio', TRUE, 'instagram', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 69 DAY), @SEED_NOW),
    (920019, '경희대 스포츠과학. 풋살 크루장으로 경기 일정 조율을 맡고 있습니다.', 2022, 'https://i.pravatar.cc/150?u=920019', NULL, NULL, NULL, 'https://instagram.com/dain.ball.demo', NULL, TRUE, JSON_OBJECT('bio', TRUE, 'instagram', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 66 DAY), @SEED_NOW),
    (920020, '중앙대 경제. 취업스터디와 금융권 인턴 정보에 관심이 많습니다.', 2021, 'https://i.pravatar.cc/150?u=920020', NULL, 'https://www.notion.so/cluverse/rowoon-econ', NULL, NULL, NULL, TRUE, JSON_OBJECT('bio', TRUE, 'notion', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 62 DAY), @SEED_NOW),
    (920021, '서강대 미디어. 오프라인 영어회화 번개 위주로 활동합니다.', 2024, 'https://i.pravatar.cc/150?u=920021', NULL, NULL, NULL, 'https://instagram.com/bora.english.demo', NULL, TRUE, JSON_OBJECT('bio', TRUE, 'instagram', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 58 DAY), @SEED_NOW),
    (920022, '홍대 미디어. 영상 촬영과 편집 파이프라인 정리에 익숙합니다.', 2022, 'https://i.pravatar.cc/150?u=920022', NULL, 'https://www.notion.so/cluverse/hangyeol-media', 'https://picsum.photos/seed/portfolio-hangyeol/800/600', NULL, NULL, TRUE, JSON_OBJECT('bio', TRUE, 'portfolio', TRUE, 'notion', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 54 DAY), @SEED_NOW),
    (920023, 'KAIST 전자전기. 로봇과 AI 시스템 연동 쪽을 주로 다룹니다.', 2021, 'https://i.pravatar.cc/150?u=920023', 'https://github.com/jiho-robot-demo', NULL, NULL, NULL, NULL, TRUE, JSON_OBJECT('bio', TRUE, 'github', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 50 DAY), @SEED_NOW),
    (920024, '클루버스 운영 관리자. 인증 검수와 커뮤니티 운영 정책을 관리합니다.', 2017, 'https://i.pravatar.cc/150?u=920024', NULL, NULL, NULL, NULL, 'https://picsum.photos/seed/ops-admin/800/600', TRUE, JSON_OBJECT('bio', TRUE, 'etc', TRUE), DATE_SUB(@SEED_NOW, INTERVAL 200 DAY), @SEED_NOW);

INSERT INTO social_account (
    member_id,
    provider,
    provider_user_id,
    created_at,
    updated_at
) VALUES
    (920001, 'KAKAO', 'kakao-920001', DATE_SUB(@SEED_NOW, INTERVAL 140 DAY), @SEED_NOW),
    (920002, 'GOOGLE', 'google-920002', DATE_SUB(@SEED_NOW, INTERVAL 132 DAY), @SEED_NOW),
    (920005, 'KAKAO', 'kakao-920005', DATE_SUB(@SEED_NOW, INTERVAL 119 DAY), @SEED_NOW),
    (920006, 'NAVER', 'naver-920006', DATE_SUB(@SEED_NOW, INTERVAL 116 DAY), @SEED_NOW),
    (920008, 'GOOGLE', 'google-920008', DATE_SUB(@SEED_NOW, INTERVAL 108 DAY), @SEED_NOW),
    (920010, 'KAKAO', 'kakao-920010', DATE_SUB(@SEED_NOW, INTERVAL 99 DAY), @SEED_NOW),
    (920011, 'KAKAO', 'kakao-920011', DATE_SUB(@SEED_NOW, INTERVAL 95 DAY), @SEED_NOW),
    (920013, 'APPLE', 'apple-920013', DATE_SUB(@SEED_NOW, INTERVAL 88 DAY), @SEED_NOW),
    (920014, 'GOOGLE', 'google-920014', DATE_SUB(@SEED_NOW, INTERVAL 84 DAY), @SEED_NOW),
    (920016, 'GOOGLE', 'google-920016', DATE_SUB(@SEED_NOW, INTERVAL 75 DAY), @SEED_NOW),
    (920021, 'KAKAO', 'kakao-920021', DATE_SUB(@SEED_NOW, INTERVAL 58 DAY), @SEED_NOW),
    (920024, 'GOOGLE', 'google-920024', DATE_SUB(@SEED_NOW, INTERVAL 200 DAY), @SEED_NOW);

INSERT INTO member_status_history (
    member_id,
    previous_status,
    new_status,
    change_type,
    change_reason,
    changed_by,
    source_system,
    client_ip,
    created_at,
    updated_at
)
SELECT
    member_id,
    'NONE',
    status,
    'SIGNUP',
    '실서비스형 샘플 초기 가입 데이터',
    NULL,
    source_system,
    client_ip,
    created_at,
    updated_at
FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

INSERT INTO student_verification (
    member_id,
    university_id,
    status,
    method,
    school_email,
    rejected_reason,
    requested_at,
    verified_at,
    created_at,
    updated_at
) VALUES
    (920001, 91001, 'APPROVED', 'SCHOOL_EMAIL', 'demo_cs_01@snu.ac.kr', NULL, DATE_SUB(@SEED_NOW, INTERVAL 140 DAY), DATE_SUB(@SEED_NOW, INTERVAL 139 DAY), DATE_SUB(@SEED_NOW, INTERVAL 140 DAY), @SEED_NOW),
    (920002, 91003, 'APPROVED', 'SCHOOL_EMAIL', 'demo_biz_01@yonsei.ac.kr', NULL, DATE_SUB(@SEED_NOW, INTERVAL 132 DAY), DATE_SUB(@SEED_NOW, INTERVAL 131 DAY), DATE_SUB(@SEED_NOW, INTERVAL 132 DAY), @SEED_NOW),
    (920003, 91002, 'APPROVED', 'STUDENT_ID_CARD', NULL, NULL, DATE_SUB(@SEED_NOW, INTERVAL 128 DAY), DATE_SUB(@SEED_NOW, INTERVAL 127 DAY), DATE_SUB(@SEED_NOW, INTERVAL 128 DAY), @SEED_NOW),
    (920004, 91007, 'APPROVED', 'SCHOOL_EMAIL', 'demo_psy_01@cau.ac.kr', NULL, DATE_SUB(@SEED_NOW, INTERVAL 121 DAY), DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), DATE_SUB(@SEED_NOW, INTERVAL 121 DAY), @SEED_NOW),
    (920005, 91010, 'APPROVED', 'ENROLLMENT_CERT', NULL, NULL, DATE_SUB(@SEED_NOW, INTERVAL 119 DAY), DATE_SUB(@SEED_NOW, INTERVAL 118 DAY), DATE_SUB(@SEED_NOW, INTERVAL 119 DAY), @SEED_NOW),
    (920006, 91005, 'APPROVED', 'SCHOOL_EMAIL', 'demo_devmusic_01@hanyang.ac.kr', NULL, DATE_SUB(@SEED_NOW, INTERVAL 116 DAY), DATE_SUB(@SEED_NOW, INTERVAL 115 DAY), DATE_SUB(@SEED_NOW, INTERVAL 116 DAY), @SEED_NOW),
    (920007, 91006, 'APPROVED', 'SCHOOL_EMAIL', 'demo_sport_01@khu.ac.kr', NULL, DATE_SUB(@SEED_NOW, INTERVAL 112 DAY), DATE_SUB(@SEED_NOW, INTERVAL 111 DAY), DATE_SUB(@SEED_NOW, INTERVAL 112 DAY), @SEED_NOW),
    (920008, 91011, 'APPROVED', 'ENROLLMENT_CERT', NULL, NULL, DATE_SUB(@SEED_NOW, INTERVAL 108 DAY), DATE_SUB(@SEED_NOW, INTERVAL 107 DAY), DATE_SUB(@SEED_NOW, INTERVAL 108 DAY), @SEED_NOW),
    (920009, 91004, 'APPROVED', 'SCHOOL_EMAIL', 'demo_mkt_01@skku.edu', NULL, DATE_SUB(@SEED_NOW, INTERVAL 104 DAY), DATE_SUB(@SEED_NOW, INTERVAL 103 DAY), DATE_SUB(@SEED_NOW, INTERVAL 104 DAY), @SEED_NOW),
    (920010, 91012, 'APPROVED', 'STUDENT_ID_CARD', NULL, NULL, DATE_SUB(@SEED_NOW, INTERVAL 99 DAY), DATE_SUB(@SEED_NOW, INTERVAL 98 DAY), DATE_SUB(@SEED_NOW, INTERVAL 99 DAY), @SEED_NOW),
    (920012, 91012, 'APPROVED', 'SCHOOL_EMAIL', 'demo_art_01@hongik.ac.kr', NULL, DATE_SUB(@SEED_NOW, INTERVAL 92 DAY), DATE_SUB(@SEED_NOW, INTERVAL 91 DAY), DATE_SUB(@SEED_NOW, INTERVAL 92 DAY), @SEED_NOW),
    (920013, 91009, 'APPROVED', 'SCHOOL_EMAIL', 'demo_backend_01@pusan.ac.kr', NULL, DATE_SUB(@SEED_NOW, INTERVAL 88 DAY), DATE_SUB(@SEED_NOW, INTERVAL 87 DAY), DATE_SUB(@SEED_NOW, INTERVAL 88 DAY), @SEED_NOW),
    (920014, 91001, 'APPROVED', 'SCHOOL_EMAIL', 'demo_front_01@snu.ac.kr', NULL, DATE_SUB(@SEED_NOW, INTERVAL 84 DAY), DATE_SUB(@SEED_NOW, INTERVAL 83 DAY), DATE_SUB(@SEED_NOW, INTERVAL 84 DAY), @SEED_NOW),
    (920015, 91002, 'APPROVED', 'SCHOOL_EMAIL', 'demo_brand_01@korea.ac.kr', NULL, DATE_SUB(@SEED_NOW, INTERVAL 80 DAY), DATE_SUB(@SEED_NOW, INTERVAL 79 DAY), DATE_SUB(@SEED_NOW, INTERVAL 80 DAY), @SEED_NOW),
    (920016, 91003, 'PENDING', 'ENROLLMENT_CERT', NULL, NULL, DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), NULL, DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW),
    (920018, 91005, 'APPROVED', 'SCHOOL_EMAIL', 'demo_drum_01@hanyang.ac.kr', NULL, DATE_SUB(@SEED_NOW, INTERVAL 69 DAY), DATE_SUB(@SEED_NOW, INTERVAL 68 DAY), DATE_SUB(@SEED_NOW, INTERVAL 69 DAY), @SEED_NOW),
    (920019, 91006, 'APPROVED', 'SCHOOL_EMAIL', 'demo_ball_01@khu.ac.kr', NULL, DATE_SUB(@SEED_NOW, INTERVAL 66 DAY), DATE_SUB(@SEED_NOW, INTERVAL 65 DAY), DATE_SUB(@SEED_NOW, INTERVAL 66 DAY), @SEED_NOW),
    (920020, 91007, 'APPROVED', 'SCHOOL_EMAIL', 'demo_econ_01@cau.ac.kr', NULL, DATE_SUB(@SEED_NOW, INTERVAL 62 DAY), DATE_SUB(@SEED_NOW, INTERVAL 61 DAY), DATE_SUB(@SEED_NOW, INTERVAL 62 DAY), @SEED_NOW),
    (920022, 91012, 'APPROVED', 'STUDENT_ID_CARD', NULL, NULL, DATE_SUB(@SEED_NOW, INTERVAL 54 DAY), DATE_SUB(@SEED_NOW, INTERVAL 53 DAY), DATE_SUB(@SEED_NOW, INTERVAL 54 DAY), @SEED_NOW),
    (920023, 91010, 'APPROVED', 'SCHOOL_EMAIL', 'demo_robot_01@kaist.ac.kr', NULL, DATE_SUB(@SEED_NOW, INTERVAL 50 DAY), DATE_SUB(@SEED_NOW, INTERVAL 49 DAY), DATE_SUB(@SEED_NOW, INTERVAL 50 DAY), @SEED_NOW);

INSERT INTO member_terms_agreement (
    member_id,
    terms_id,
    agreed_at,
    created_at,
    updated_at
)
SELECT
    m.member_id,
    t.terms_id,
    GREATEST(m.created_at, DATE_SUB(@SEED_NOW, INTERVAL 50 DAY)),
    GREATEST(m.created_at, DATE_SUB(@SEED_NOW, INTERVAL 50 DAY)),
    @SEED_NOW
FROM member m
JOIN terms t
  ON t.terms_id BETWEEN @TERMS_START_ID AND @TERMS_END_ID
WHERE m.member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

INSERT INTO notification_setting (
    member_id,
    push_enabled,
    comment_on_post,
    reply_on_comment,
    application_result,
    application_received,
    group_notice,
    follow_activity,
    created_at,
    updated_at
)
SELECT
    member_id,
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    CASE
        WHEN member_id IN (920001, 920002, 920006, 920008, 920010, 920013, 920019, 920024) THEN TRUE
        ELSE FALSE
    END,
    created_at,
    @SEED_NOW
FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

INSERT INTO notification_preference (
    member_id,
    comments,
    group_notifications,
    announcements,
    follows,
    marketing
)
SELECT
    member_id,
    TRUE,
    TRUE,
    CASE
        WHEN member_id IN (920001, 920002, 920008, 920014, 920024) THEN TRUE
        ELSE FALSE
    END,
    CASE
        WHEN member_id IN (920001, 920006, 920010, 920019, 920024) THEN TRUE
        ELSE FALSE
    END,
    CASE
        WHEN member_id IN (920002, 920005, 920010, 920015) THEN TRUE
        ELSE FALSE
    END
FROM member
WHERE member_id BETWEEN @MEMBER_START_ID AND @MEMBER_END_ID;

INSERT INTO notification (
    notification_id,
    member_id,
    type,
    title,
    content,
    excerpt,
    is_read,
    target_url,
    created_at,
    updated_at
) VALUES
    (990001, 920001, 'COMMENT', '새 댓글이 달렸습니다', '개발자 라운지 모집 글에 새 댓글이 달렸습니다.', '백엔드 포지션이면 Java/Spring 기준인가요?', FALSE, '/post/970001', DATE_SUB(@SEED_NOW, INTERVAL 11 DAY), @SEED_NOW),
    (990002, 920006, 'GROUP_ANNOUNCEMENT', '모임 공지가 등록되었습니다', '안암 밴드클럽에 새 공지가 등록되었습니다.', '다음 합주곡 후보 받습니다', TRUE, '/post/970013', DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (990003, 920010, 'FOLLOW', '새 팔로워가 생겼습니다', '유나브랜딩님이 회원님을 팔로우하기 시작했습니다.', '브랜딩/포트폴리오 글을 자주 보고 있어요.', FALSE, '/members/920015/profile', DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW),
    (990004, 920008, 'FOLLOWING_POST', '팔로우한 사용자의 새 글', '시우AI님이 새 게시글을 작성했습니다.', 'MLOps 세미나 같이 갈 사람', FALSE, '/post/970018', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (990005, 920002, 'GROUP_APPROVED', '모임 지원이 승인되었습니다', '캠퍼스 프로덕트 랩 지원이 승인되었습니다.', '스프린트 미팅부터 바로 참여 가능합니다.', TRUE, '/group/960004', DATE_SUB(@SEED_NOW, INTERVAL 15 DAY), @SEED_NOW),
    (990006, 920016, 'REPLY', '댓글에 답글이 달렸습니다', '작성한 댓글에 답글이 달렸습니다.', '네, 대신 질문 하나씩 준비해오면 좋겠습니다.', FALSE, '/post/970008', DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (990007, 920019, 'GROUP_REJECTED', '모임 지원 결과를 확인해주세요', '대학연합 풋살 크루 지원이 보류되었습니다.', '다음 정기 모집 때 다시 지원 부탁드립니다.', TRUE, '/group/960006', DATE_SUB(@SEED_NOW, INTERVAL 22 DAY), @SEED_NOW),
    (990008, 920024, 'COMMENT', '운영 공지 글에 댓글이 달렸습니다', '운영 공지 글에 새 댓글이 달렸습니다.', '세션 자료 감사합니다. 이번 주는 인증 파트 먼저 보겠습니다.', FALSE, '/post/970005', DATE_SUB(@SEED_NOW, INTERVAL 8 DAY), @SEED_NOW);

INSERT INTO member_report (
    member_report_id,
    reporter_id,
    target_type,
    target_id,
    reason_code,
    detail,
    status,
    created_at,
    updated_at
) VALUES
    (991001, 920014, 'POST', 970016, 'SPAM', '동일한 영어회화 모집 글이 외부 오픈채팅 링크와 함께 여러 번 반복 등록된 것처럼 보여 확인 요청드립니다.', 'IN_REVIEW', DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (991002, 920012, 'MEMBER', 920021, 'ABUSE', '오프라인 번개 안내 과정에서 과한 표현이 있었다는 제보를 받아 신고합니다. 대화 맥락 확인 부탁드립니다.', 'RECEIVED', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (991003, 920024, 'GROUP', 960006, 'ETC', '풋살 크루 모집 소개 문구에 외부 참가자 제한 여부가 불명확해서 운영 기준 점검 차원에서 등록했습니다.', 'RESOLVED', DATE_SUB(@SEED_NOW, INTERVAL 12 DAY), @SEED_NOW);

INSERT INTO member_report_evidence_image (
    member_report_id,
    image_url
) VALUES
    (991001, 'https://picsum.photos/seed/report-991001-1/1200/800'),
    (991001, 'https://picsum.photos/seed/report-991001-2/1200/800'),
    (991002, 'https://picsum.photos/seed/report-991002-1/1200/800');

INSERT INTO board (
    board_id,
    board_type,
    name,
    description,
    parent_id,
    depth,
    display_order,
    is_active,
    created_at,
    updated_at
) VALUES
    (930001, 'DEPARTMENT', '공학계열', '공학 계열 전공 보드 그룹', NULL, 0, 1, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930002, 'DEPARTMENT', '경상사회계열', '경영, 경제, 심리 계열 보드 그룹', NULL, 0, 2, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930003, 'DEPARTMENT', '예체능계열', '디자인, 스포츠, 미디어 계열 보드 그룹', NULL, 0, 3, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930004, 'DEPARTMENT', '컴퓨터공학과', '백엔드와 시스템, 알고리즘 이야기가 많은 전공 보드', 930001, 1, 4, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930005, 'DEPARTMENT', '소프트웨어학부', '웹과 앱 서비스 개발 중심의 전공 보드', 930001, 1, 5, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930006, 'DEPARTMENT', '전자전기공학부', '회로, 임베디드, 로봇 시스템 중심 보드', 930001, 1, 6, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930007, 'DEPARTMENT', '인공지능학과', '모델 실험, 논문, 데이터셋 이야기가 많은 보드', 930001, 1, 7, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930008, 'DEPARTMENT', '경영학과', '프로덕트, 운영, 스타트업 관련 이야기가 많은 보드', 930002, 1, 8, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930009, 'DEPARTMENT', '경제학부', '산업 분석, 금융, 데이터 해석 중심의 보드', 930002, 1, 9, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930010, 'DEPARTMENT', '심리학과', '유저 리서치와 행동 이해 관련 글이 많은 보드', 930002, 1, 10, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930011, 'DEPARTMENT', '시각디자인학과', '브랜딩, UI, 포스터 작업 중심의 보드', 930003, 1, 11, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930012, 'DEPARTMENT', '스포츠과학과', '러닝과 구기 종목, 팀 운영 팁을 나누는 보드', 930003, 1, 12, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930013, 'DEPARTMENT', '미디어커뮤니케이션학과', '영상 제작과 콘텐츠 기획 중심의 보드', 930003, 1, 13, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930014, 'INTEREST', '개발', '개발자 커뮤니티 상위 카테고리', NULL, 0, 14, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930015, 'INTEREST', '창업', '스타트업과 제품 기획 상위 카테고리', NULL, 0, 15, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930016, 'INTEREST', '예술', '밴드, 미술 등 문화예술 상위 카테고리', NULL, 0, 16, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930017, 'INTEREST', '스포츠', '풋살, 러닝 등 스포츠 상위 카테고리', NULL, 0, 17, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930018, 'INTEREST', '커리어', '취업 준비와 포트폴리오 상위 카테고리', NULL, 0, 18, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930019, 'INTEREST', '언어교환', '영어, 일본어 회화 상위 카테고리', NULL, 0, 19, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930020, 'INTEREST', '백엔드', '서버 개발과 아키텍처 이야기', 930014, 1, 20, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930021, 'INTEREST', '프론트엔드', '웹 UI와 인터랙션 이야기', 930014, 1, 21, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930022, 'INTEREST', 'AI/데이터', '모델링과 실험 기록, 데이터 처리 이야기', 930014, 1, 22, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930023, 'INTEREST', '스타트업', '시장 검증과 팀 빌딩 이야기', 930015, 1, 23, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930024, 'INTEREST', '브랜딩', '브랜드 메시지와 비주얼 정리', 930015, 1, 24, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930025, 'INTEREST', '밴드', '합주, 공연, 장비 이야기', 930016, 1, 25, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930026, 'INTEREST', '드로잉/미술', '크로키, 전시, 작업 노트 이야기', 930016, 1, 26, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930027, 'INTEREST', '축구/풋살', '매치, 포지션, 구장 정보', 930017, 1, 27, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930028, 'INTEREST', '러닝', '러닝 루트와 기록 공유', 930017, 1, 28, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930029, 'INTEREST', '취업스터디', '인턴, 자소서, 면접 준비 이야기', 930018, 1, 29, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930030, 'INTEREST', '포트폴리오', '포트폴리오와 이력서 피드백', 930018, 1, 30, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930031, 'INTEREST', '영어회화', '영어 스피킹과 번개 모임', 930019, 1, 31, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930032, 'INTEREST', '일본어회화', '일본어 회화와 문화 교류', 930019, 1, 32, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (930033, 'GROUP', '서울대 개발자 라운지', '캠퍼스 개발자들이 코드 리뷰와 사이드프로젝트를 하는 모임', NULL, 0, 33, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 100 DAY), @SEED_NOW),
    (930034, 'GROUP', '안암 밴드클럽', '고려대 인근에서 정기 합주와 공연을 준비하는 밴드 모임', NULL, 0, 34, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 95 DAY), @SEED_NOW),
    (930035, 'GROUP', '한강 러닝 메이트', '평일 저녁과 주말 러닝 번개 중심의 소모임', NULL, 0, 35, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 90 DAY), @SEED_NOW),
    (930036, 'GROUP', '캠퍼스 프로덕트 랩', '대학연합 PM, 디자이너, 개발자 프로젝트 팀', NULL, 0, 36, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 88 DAY), @SEED_NOW),
    (930037, 'GROUP', '홍대 드로잉 살롱', '홍대권에서 크로키와 드로잉 피드백을 진행하는 모임', NULL, 0, 37, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 84 DAY), @SEED_NOW),
    (930038, 'GROUP', '대학연합 풋살 크루', '서울권 대학생들이 주말 풋살 경기를 여는 크루', NULL, 0, 38, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 80 DAY), @SEED_NOW),
    (930039, 'GROUP', '부산 백엔드 스터디', '부산권 개발자들이 오프라인과 온라인으로 병행하는 스터디', NULL, 0, 39, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 76 DAY), @SEED_NOW),
    (930040, 'GROUP', 'AI 논문 읽기 모임', '매주 한 편씩 논문을 정리하고 발표하는 온라인 스터디', NULL, 0, 40, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 72 DAY), @SEED_NOW);

INSERT INTO major (
    major_id,
    board_id,
    name,
    parent_id,
    depth,
    display_order,
    is_active,
    created_at,
    updated_at
) VALUES
    (940001, 930001, '공학계열', NULL, 0, 1, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (940002, 930002, '경상사회계열', NULL, 0, 2, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (940003, 930003, '예체능계열', NULL, 0, 3, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (940004, 930004, '컴퓨터공학과', 940001, 1, 4, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (940005, 930005, '소프트웨어학부', 940001, 1, 5, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (940006, 930006, '전자전기공학부', 940001, 1, 6, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (940007, 930007, '인공지능학과', 940001, 1, 7, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (940008, 930008, '경영학과', 940002, 1, 8, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (940009, 930009, '경제학부', 940002, 1, 9, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (940010, 930010, '심리학과', 940002, 1, 10, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (940011, 930011, '시각디자인학과', 940003, 1, 11, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (940012, 930012, '스포츠과학과', 940003, 1, 12, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (940013, 930013, '미디어커뮤니케이션학과', 940003, 1, 13, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW);

INSERT INTO member_major (
    member_id,
    major_id,
    major_type,
    created_at,
    updated_at
) VALUES
    (920001, 940004, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 140 DAY), @SEED_NOW),
    (920002, 940008, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 132 DAY), @SEED_NOW),
    (920003, 940006, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 128 DAY), @SEED_NOW),
    (920004, 940010, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 121 DAY), @SEED_NOW),
    (920005, 940007, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 119 DAY), @SEED_NOW),
    (920005, 940008, 'DOUBLE_MAJOR', DATE_SUB(@SEED_NOW, INTERVAL 90 DAY), @SEED_NOW),
    (920006, 940005, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 116 DAY), @SEED_NOW),
    (920007, 940012, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 112 DAY), @SEED_NOW),
    (920008, 940007, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 108 DAY), @SEED_NOW),
    (920009, 940009, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 104 DAY), @SEED_NOW),
    (920009, 940008, 'DOUBLE_MAJOR', DATE_SUB(@SEED_NOW, INTERVAL 80 DAY), @SEED_NOW),
    (920010, 940011, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 99 DAY), @SEED_NOW),
    (920010, 940013, 'DOUBLE_MAJOR', DATE_SUB(@SEED_NOW, INTERVAL 70 DAY), @SEED_NOW),
    (920011, 940009, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 95 DAY), @SEED_NOW),
    (920012, 940013, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 92 DAY), @SEED_NOW),
    (920013, 940005, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 88 DAY), @SEED_NOW),
    (920014, 940004, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 84 DAY), @SEED_NOW),
    (920015, 940008, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 80 DAY), @SEED_NOW),
    (920016, 940007, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 75 DAY), @SEED_NOW),
    (920017, 940010, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 71 DAY), @SEED_NOW),
    (920018, 940006, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 69 DAY), @SEED_NOW),
    (920019, 940012, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 66 DAY), @SEED_NOW),
    (920020, 940009, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 62 DAY), @SEED_NOW),
    (920021, 940013, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 58 DAY), @SEED_NOW),
    (920022, 940013, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 54 DAY), @SEED_NOW),
    (920023, 940006, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 50 DAY), @SEED_NOW),
    (920023, 940007, 'DOUBLE_MAJOR', DATE_SUB(@SEED_NOW, INTERVAL 40 DAY), @SEED_NOW),
    (920024, 940004, 'PRIMARY', DATE_SUB(@SEED_NOW, INTERVAL 200 DAY), @SEED_NOW);

INSERT INTO interest (
    interest_id,
    board_id,
    name,
    category,
    parent_id,
    display_order,
    is_active,
    created_at,
    updated_at
) VALUES
    (950001, 930014, '개발', 'TECH', NULL, 1, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950002, 930015, '창업', 'BUSINESS', NULL, 2, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950003, 930016, '예술', 'ART', NULL, 3, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950004, 930017, '스포츠', 'SPORTS', NULL, 4, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950005, 930018, '커리어', 'CAREER', NULL, 5, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950006, 930019, '언어교환', 'LANGUAGE', NULL, 6, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950007, 930020, '백엔드', 'TECH', 950001, 7, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950008, 930021, '프론트엔드', 'TECH', 950001, 8, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950009, 930022, 'AI/데이터', 'TECH', 950001, 9, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950010, 930023, '스타트업', 'BUSINESS', 950002, 10, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950011, 930024, '브랜딩', 'BUSINESS', 950002, 11, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950012, 930025, '밴드', 'ART', 950003, 12, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950013, 930026, '드로잉/미술', 'ART', 950003, 13, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950014, 930027, '축구/풋살', 'SPORTS', 950004, 14, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950015, 930028, '러닝', 'SPORTS', 950004, 15, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950016, 930029, '취업스터디', 'CAREER', 950005, 16, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950017, 930030, '포트폴리오', 'CAREER', 950005, 17, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950018, 930031, '영어회화', 'LANGUAGE', 950006, 18, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW),
    (950019, 930032, '일본어회화', 'LANGUAGE', 950006, 19, TRUE, DATE_SUB(@SEED_NOW, INTERVAL 150 DAY), @SEED_NOW);

INSERT INTO interest_major_relation (
    interest_id,
    major_id,
    created_at,
    updated_at
) VALUES
    (950007, 940004, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950007, 940005, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950008, 940004, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950008, 940011, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950009, 940007, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950009, 940006, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950010, 940008, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950010, 940009, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950011, 940008, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950011, 940011, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950012, 940013, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950013, 940011, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950013, 940013, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950014, 940012, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950015, 940012, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950016, 940008, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950016, 940009, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950017, 940011, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950017, 940013, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950018, 940013, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW),
    (950019, 940013, DATE_SUB(@SEED_NOW, INTERVAL 120 DAY), @SEED_NOW);

INSERT INTO member_interests (
    member_id,
    interest_id
) VALUES
    (920001, 950007), (920001, 950010),
    (920002, 950010), (920002, 950011),
    (920003, 950009), (920003, 950014),
    (920004, 950017), (920004, 950013),
    (920005, 950009), (920005, 950010),
    (920006, 950012), (920006, 950008),
    (920007, 950014), (920007, 950015),
    (920008, 950009), (920008, 950016),
    (920009, 950011), (920009, 950016),
    (920010, 950013), (920010, 950017),
    (920011, 950015), (920011, 950014),
    (920012, 950013), (920012, 950012),
    (920013, 950007), (920013, 950016),
    (920014, 950008), (920014, 950017),
    (920015, 950011), (920015, 950013),
    (920016, 950009), (920016, 950008),
    (920017, 950016), (920017, 950018),
    (920018, 950012), (920018, 950009),
    (920019, 950014), (920019, 950015),
    (920020, 950016), (920020, 950018),
    (920021, 950018), (920021, 950019),
    (920022, 950013), (920022, 950011),
    (920023, 950009), (920023, 950007),
    (920024, 950007), (920024, 950016);

INSERT INTO `group` (
    group_id,
    board_id,
    name,
    description,
    cover_image_url,
    category,
    activity_type,
    region,
    visibility,
    status,
    owner_id,
    max_members,
    member_count,
    created_at,
    updated_at
) VALUES
    (960001, 930033, '서울대 개발자 라운지', '서울대 중심으로 시작했지만 대학연합으로 확장된 개발자 프로젝트 모임입니다.', 'https://picsum.photos/seed/group-dev/800/400', 'PROJECT', 'HYBRID', '서울', 'PUBLIC', 'ACTIVE', 920001, 12, 6, DATE_SUB(@SEED_NOW, INTERVAL 100 DAY), @SEED_NOW),
    (960002, 930034, '안암 밴드클럽', '정기 합주와 학기말 공연을 준비하는 대학생 밴드 모임입니다.', 'https://picsum.photos/seed/group-band/800/400', 'CLUB', 'OFFLINE', '서울', 'PUBLIC', 'ACTIVE', 920006, 20, 5, DATE_SUB(@SEED_NOW, INTERVAL 95 DAY), @SEED_NOW),
    (960003, 930035, '한강 러닝 메이트', '퇴근 후 짧은 러닝과 주말 장거리 러닝을 함께하는 소모임입니다.', 'https://picsum.photos/seed/group-running/800/400', 'SMALL_GROUP', 'OFFLINE', '서울', 'PUBLIC', 'ACTIVE', 920011, 30, 5, DATE_SUB(@SEED_NOW, INTERVAL 90 DAY), @SEED_NOW),
    (960004, 930036, '캠퍼스 프로덕트 랩', 'PM, 디자이너, 개발자가 실제 사용자 문제를 풀어보는 대학연합 팀입니다.', 'https://picsum.photos/seed/group-product/800/400', 'PROJECT', 'HYBRID', '서울', 'PUBLIC', 'ACTIVE', 920002, 10, 6, DATE_SUB(@SEED_NOW, INTERVAL 88 DAY), @SEED_NOW),
    (960005, 930037, '홍대 드로잉 살롱', '크로키와 아이패드 드로잉 피드백을 주고받는 오프라인 중심 모임입니다.', 'https://picsum.photos/seed/group-drawing/800/400', 'CLUB', 'OFFLINE', '서울', 'PUBLIC', 'ACTIVE', 920010, 15, 5, DATE_SUB(@SEED_NOW, INTERVAL 84 DAY), @SEED_NOW),
    (960006, 930038, '대학연합 풋살 크루', '주말 고정 풋살과 월 1회 교류전을 여는 대학연합 스포츠 크루입니다.', 'https://picsum.photos/seed/group-futsal/800/400', 'SMALL_GROUP', 'OFFLINE', '서울', 'PUBLIC', 'ACTIVE', 920019, 24, 6, DATE_SUB(@SEED_NOW, INTERVAL 80 DAY), @SEED_NOW),
    (960007, 930039, '부산 백엔드 스터디', '부산 지역 개발자들이 실무 중심으로 공부하는 백엔드 스터디입니다.', 'https://picsum.photos/seed/group-backend/800/400', 'STUDY', 'HYBRID', '부산', 'PARTIAL', 'ACTIVE', 920013, 8, 4, DATE_SUB(@SEED_NOW, INTERVAL 76 DAY), @SEED_NOW),
    (960008, 930040, 'AI 논문 읽기 모임', '매주 논문 한 편을 읽고 발표하는 온라인 스터디입니다.', 'https://picsum.photos/seed/group-ai-paper/800/400', 'STUDY', 'ONLINE', NULL, 'PUBLIC', 'ACTIVE', 920008, 12, 5, DATE_SUB(@SEED_NOW, INTERVAL 72 DAY), @SEED_NOW);

INSERT INTO group_role (
    group_role_id,
    group_id,
    title,
    display_order,
    created_at,
    updated_at
) VALUES
    (961001, 960001, 'Backend Lead', 1, DATE_SUB(@SEED_NOW, INTERVAL 95 DAY), @SEED_NOW),
    (961002, 960001, 'Frontend Lead', 2, DATE_SUB(@SEED_NOW, INTERVAL 95 DAY), @SEED_NOW),
    (961003, 960002, '보컬', 1, DATE_SUB(@SEED_NOW, INTERVAL 90 DAY), @SEED_NOW),
    (961004, 960002, '드럼', 2, DATE_SUB(@SEED_NOW, INTERVAL 90 DAY), @SEED_NOW),
    (961005, 960004, 'PM', 1, DATE_SUB(@SEED_NOW, INTERVAL 85 DAY), @SEED_NOW),
    (961006, 960004, 'Designer', 2, DATE_SUB(@SEED_NOW, INTERVAL 85 DAY), @SEED_NOW),
    (961007, 960006, '주장', 1, DATE_SUB(@SEED_NOW, INTERVAL 78 DAY), @SEED_NOW),
    (961008, 960008, '발표 담당', 1, DATE_SUB(@SEED_NOW, INTERVAL 70 DAY), @SEED_NOW);

INSERT INTO group_interest (
    group_id,
    interest_id,
    created_at,
    updated_at
) VALUES
    (960001, 950007, DATE_SUB(@SEED_NOW, INTERVAL 100 DAY), @SEED_NOW),
    (960001, 950008, DATE_SUB(@SEED_NOW, INTERVAL 100 DAY), @SEED_NOW),
    (960002, 950012, DATE_SUB(@SEED_NOW, INTERVAL 95 DAY), @SEED_NOW),
    (960002, 950013, DATE_SUB(@SEED_NOW, INTERVAL 95 DAY), @SEED_NOW),
    (960003, 950015, DATE_SUB(@SEED_NOW, INTERVAL 90 DAY), @SEED_NOW),
    (960003, 950014, DATE_SUB(@SEED_NOW, INTERVAL 90 DAY), @SEED_NOW),
    (960004, 950010, DATE_SUB(@SEED_NOW, INTERVAL 88 DAY), @SEED_NOW),
    (960004, 950017, DATE_SUB(@SEED_NOW, INTERVAL 88 DAY), @SEED_NOW),
    (960005, 950013, DATE_SUB(@SEED_NOW, INTERVAL 84 DAY), @SEED_NOW),
    (960005, 950017, DATE_SUB(@SEED_NOW, INTERVAL 84 DAY), @SEED_NOW),
    (960006, 950014, DATE_SUB(@SEED_NOW, INTERVAL 80 DAY), @SEED_NOW),
    (960006, 950015, DATE_SUB(@SEED_NOW, INTERVAL 80 DAY), @SEED_NOW),
    (960007, 950007, DATE_SUB(@SEED_NOW, INTERVAL 76 DAY), @SEED_NOW),
    (960007, 950016, DATE_SUB(@SEED_NOW, INTERVAL 76 DAY), @SEED_NOW),
    (960008, 950009, DATE_SUB(@SEED_NOW, INTERVAL 72 DAY), @SEED_NOW),
    (960008, 950016, DATE_SUB(@SEED_NOW, INTERVAL 72 DAY), @SEED_NOW);

INSERT INTO group_member (
    group_id,
    member_id,
    role,
    custom_title_id,
    joined_at,
    created_at,
    updated_at
) VALUES
    (960001, 920001, 'OWNER', 961001, DATE_SUB(@SEED_NOW, INTERVAL 100 DAY), DATE_SUB(@SEED_NOW, INTERVAL 100 DAY), @SEED_NOW),
    (960001, 920014, 'ADMIN', 961002, DATE_SUB(@SEED_NOW, INTERVAL 84 DAY), DATE_SUB(@SEED_NOW, INTERVAL 84 DAY), @SEED_NOW),
    (960001, 920016, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 72 DAY), DATE_SUB(@SEED_NOW, INTERVAL 72 DAY), @SEED_NOW),
    (960001, 920013, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 68 DAY), DATE_SUB(@SEED_NOW, INTERVAL 68 DAY), @SEED_NOW),
    (960001, 920023, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 49 DAY), DATE_SUB(@SEED_NOW, INTERVAL 49 DAY), @SEED_NOW),
    (960001, 920005, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 47 DAY), DATE_SUB(@SEED_NOW, INTERVAL 47 DAY), @SEED_NOW),
    (960002, 920006, 'OWNER', 961003, DATE_SUB(@SEED_NOW, INTERVAL 95 DAY), DATE_SUB(@SEED_NOW, INTERVAL 95 DAY), @SEED_NOW),
    (960002, 920018, 'ADMIN', 961004, DATE_SUB(@SEED_NOW, INTERVAL 69 DAY), DATE_SUB(@SEED_NOW, INTERVAL 69 DAY), @SEED_NOW),
    (960002, 920012, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 67 DAY), DATE_SUB(@SEED_NOW, INTERVAL 67 DAY), @SEED_NOW),
    (960002, 920015, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 63 DAY), DATE_SUB(@SEED_NOW, INTERVAL 63 DAY), @SEED_NOW),
    (960002, 920021, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 54 DAY), DATE_SUB(@SEED_NOW, INTERVAL 54 DAY), @SEED_NOW),
    (960003, 920011, 'OWNER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 90 DAY), DATE_SUB(@SEED_NOW, INTERVAL 90 DAY), @SEED_NOW),
    (960003, 920019, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 66 DAY), DATE_SUB(@SEED_NOW, INTERVAL 66 DAY), @SEED_NOW),
    (960003, 920007, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 64 DAY), DATE_SUB(@SEED_NOW, INTERVAL 64 DAY), @SEED_NOW),
    (960003, 920020, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 58 DAY), DATE_SUB(@SEED_NOW, INTERVAL 58 DAY), @SEED_NOW),
    (960003, 920021, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 52 DAY), DATE_SUB(@SEED_NOW, INTERVAL 52 DAY), @SEED_NOW),
    (960004, 920002, 'OWNER', 961005, DATE_SUB(@SEED_NOW, INTERVAL 88 DAY), DATE_SUB(@SEED_NOW, INTERVAL 88 DAY), @SEED_NOW),
    (960004, 920015, 'ADMIN', NULL, DATE_SUB(@SEED_NOW, INTERVAL 79 DAY), DATE_SUB(@SEED_NOW, INTERVAL 79 DAY), @SEED_NOW),
    (960004, 920009, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 73 DAY), DATE_SUB(@SEED_NOW, INTERVAL 73 DAY), @SEED_NOW),
    (960004, 920010, 'MEMBER', 961006, DATE_SUB(@SEED_NOW, INTERVAL 70 DAY), DATE_SUB(@SEED_NOW, INTERVAL 70 DAY), @SEED_NOW),
    (960004, 920005, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 66 DAY), DATE_SUB(@SEED_NOW, INTERVAL 66 DAY), @SEED_NOW),
    (960004, 920014, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 60 DAY), DATE_SUB(@SEED_NOW, INTERVAL 60 DAY), @SEED_NOW),
    (960005, 920010, 'OWNER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 84 DAY), DATE_SUB(@SEED_NOW, INTERVAL 84 DAY), @SEED_NOW),
    (960005, 920012, 'ADMIN', NULL, DATE_SUB(@SEED_NOW, INTERVAL 80 DAY), DATE_SUB(@SEED_NOW, INTERVAL 80 DAY), @SEED_NOW),
    (960005, 920004, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 74 DAY), DATE_SUB(@SEED_NOW, INTERVAL 74 DAY), @SEED_NOW),
    (960005, 920022, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 53 DAY), DATE_SUB(@SEED_NOW, INTERVAL 53 DAY), @SEED_NOW),
    (960005, 920015, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 50 DAY), DATE_SUB(@SEED_NOW, INTERVAL 50 DAY), @SEED_NOW),
    (960006, 920019, 'OWNER', 961007, DATE_SUB(@SEED_NOW, INTERVAL 80 DAY), DATE_SUB(@SEED_NOW, INTERVAL 80 DAY), @SEED_NOW),
    (960006, 920007, 'ADMIN', NULL, DATE_SUB(@SEED_NOW, INTERVAL 70 DAY), DATE_SUB(@SEED_NOW, INTERVAL 70 DAY), @SEED_NOW),
    (960006, 920011, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 65 DAY), DATE_SUB(@SEED_NOW, INTERVAL 65 DAY), @SEED_NOW),
    (960006, 920020, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 60 DAY), DATE_SUB(@SEED_NOW, INTERVAL 60 DAY), @SEED_NOW),
    (960006, 920003, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 57 DAY), DATE_SUB(@SEED_NOW, INTERVAL 57 DAY), @SEED_NOW),
    (960006, 920006, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 52 DAY), DATE_SUB(@SEED_NOW, INTERVAL 52 DAY), @SEED_NOW),
    (960007, 920013, 'OWNER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 76 DAY), DATE_SUB(@SEED_NOW, INTERVAL 76 DAY), @SEED_NOW),
    (960007, 920001, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 70 DAY), DATE_SUB(@SEED_NOW, INTERVAL 70 DAY), @SEED_NOW),
    (960007, 920023, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 48 DAY), DATE_SUB(@SEED_NOW, INTERVAL 48 DAY), @SEED_NOW),
    (960007, 920008, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 46 DAY), DATE_SUB(@SEED_NOW, INTERVAL 46 DAY), @SEED_NOW),
    (960008, 920008, 'OWNER', 961008, DATE_SUB(@SEED_NOW, INTERVAL 72 DAY), DATE_SUB(@SEED_NOW, INTERVAL 72 DAY), @SEED_NOW),
    (960008, 920016, 'ADMIN', NULL, DATE_SUB(@SEED_NOW, INTERVAL 69 DAY), DATE_SUB(@SEED_NOW, INTERVAL 69 DAY), @SEED_NOW),
    (960008, 920005, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 65 DAY), DATE_SUB(@SEED_NOW, INTERVAL 65 DAY), @SEED_NOW),
    (960008, 920023, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 44 DAY), DATE_SUB(@SEED_NOW, INTERVAL 44 DAY), @SEED_NOW),
    (960008, 920024, 'MEMBER', NULL, DATE_SUB(@SEED_NOW, INTERVAL 40 DAY), DATE_SUB(@SEED_NOW, INTERVAL 40 DAY), @SEED_NOW);

INSERT INTO group_member_history (
    group_id,
    member_id,
    action,
    previous_role,
    new_role,
    reason,
    acted_by,
    source_system,
    client_ip,
    created_at,
    updated_at
)
SELECT
    gm.group_id,
    gm.member_id,
    'JOIN',
    NULL,
    gm.role,
    '실서비스형 샘플 초기 멤버십 데이터',
    g.owner_id,
    'BATCH',
    '203.0.113.210',
    gm.joined_at,
    @SEED_NOW
FROM group_member gm
JOIN `group` g
  ON g.group_id = gm.group_id
WHERE gm.group_id BETWEEN @GROUP_START_ID AND @GROUP_END_ID;

INSERT INTO follow (
    follower_id,
    following_id,
    created_at,
    updated_at
) VALUES
    (920014, 920001, DATE_SUB(@SEED_NOW, INTERVAL 30 DAY), @SEED_NOW),
    (920016, 920001, DATE_SUB(@SEED_NOW, INTERVAL 25 DAY), @SEED_NOW),
    (920005, 920002, DATE_SUB(@SEED_NOW, INTERVAL 27 DAY), @SEED_NOW),
    (920009, 920002, DATE_SUB(@SEED_NOW, INTERVAL 18 DAY), @SEED_NOW),
    (920018, 920006, DATE_SUB(@SEED_NOW, INTERVAL 20 DAY), @SEED_NOW),
    (920012, 920006, DATE_SUB(@SEED_NOW, INTERVAL 19 DAY), @SEED_NOW),
    (920021, 920006, DATE_SUB(@SEED_NOW, INTERVAL 17 DAY), @SEED_NOW),
    (920019, 920011, DATE_SUB(@SEED_NOW, INTERVAL 15 DAY), @SEED_NOW),
    (920007, 920019, DATE_SUB(@SEED_NOW, INTERVAL 15 DAY), @SEED_NOW),
    (920020, 920019, DATE_SUB(@SEED_NOW, INTERVAL 13 DAY), @SEED_NOW),
    (920010, 920012, DATE_SUB(@SEED_NOW, INTERVAL 16 DAY), @SEED_NOW),
    (920022, 920010, DATE_SUB(@SEED_NOW, INTERVAL 14 DAY), @SEED_NOW),
    (920015, 920010, DATE_SUB(@SEED_NOW, INTERVAL 14 DAY), @SEED_NOW),
    (920023, 920008, DATE_SUB(@SEED_NOW, INTERVAL 12 DAY), @SEED_NOW),
    (920005, 920008, DATE_SUB(@SEED_NOW, INTERVAL 10 DAY), @SEED_NOW),
    (920024, 920014, DATE_SUB(@SEED_NOW, INTERVAL 9 DAY), @SEED_NOW),
    (920013, 920001, DATE_SUB(@SEED_NOW, INTERVAL 11 DAY), @SEED_NOW),
    (920001, 920013, DATE_SUB(@SEED_NOW, INTERVAL 8 DAY), @SEED_NOW),
    (920021, 920002, DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (920017, 920020, DATE_SUB(@SEED_NOW, INTERVAL 6 DAY), @SEED_NOW),
    (920009, 920015, DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (920006, 920018, DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW);

INSERT INTO post (
    post_id,
    board_id,
    member_id,
    title,
    content,
    category,
    is_anonymous,
    is_pinned,
    is_external_visible,
    status,
    deleted_at,
    client_ip,
    created_at,
    updated_at
) VALUES
    (970001, 930020, 920001, '백엔드 스터디에서 Spring 1기 모집합니다', '주 1회 온라인 코드리뷰와 격주 오프라인 세션으로 진행하려고 합니다. 학교는 달라도 괜찮고, Java/Spring 기본기는 있으면 좋습니다.', 'RECRUITMENT', FALSE, FALSE, TRUE, 'ACTIVE', NULL, '198.51.100.11', DATE_SUB(@SEED_NOW, INTERVAL 12 DAY), @SEED_NOW),
    (970002, 930008, 920002, '캠퍼스 PM 세션 같이 들을 분 있나요', '이번 주말에 제품지표 정리와 인터뷰 질문 설계 세션을 열려고 합니다. PM이나 창업 관심 있는 분이면 같이 오셔도 좋습니다.', 'GENERAL', FALSE, FALSE, TRUE, 'ACTIVE', NULL, '198.51.100.12', DATE_SUB(@SEED_NOW, INTERVAL 11 DAY), @SEED_NOW),
    (970003, 930025, 920006, '합주실 정기 대관 정보 공유합니다', '신촌, 왕십리, 성수 쪽 합주실 직접 써본 곳 정리해봤습니다. 장비 상태와 시간대별 가격 차이도 같이 적었습니다.', 'INFORMATION', FALSE, FALSE, TRUE, 'ACTIVE', NULL, '198.51.100.13', DATE_SUB(@SEED_NOW, INTERVAL 10 DAY), @SEED_NOW),
    (970004, 930027, 920019, '토요일 잠실 풋살 인원 구해요', '토요일 오전 9시에 잠실 쪽에서 경기 잡혀 있습니다. 현재 8명이라 2~4명 정도 더 모이면 좋겠습니다.', 'RECRUITMENT', FALSE, FALSE, TRUE, 'ACTIVE', NULL, '198.51.100.14', DATE_SUB(@SEED_NOW, INTERVAL 9 DAY), @SEED_NOW),
    (970005, 930033, 920014, '이번 주 개발자 라운지 세션 자료 올립니다', '인증 구조와 세션 관리, 토큰 분리 전략까지 정리한 자료입니다. 세션 전에 먼저 읽고 오시면 코드리뷰가 훨씬 빨라집니다.', 'NOTICE', FALSE, TRUE, FALSE, 'ACTIVE', NULL, '198.51.100.15', DATE_SUB(@SEED_NOW, INTERVAL 8 DAY), @SEED_NOW),
    (970006, 930036, 920002, '프로덕트 랩 3월 스프린트 공지', '이번 스프린트 목표는 리텐션 인터뷰 8건, 온보딩 개선안 2개, 프로토타입 1차 검증입니다. 각자 담당 범위 확인 부탁드립니다.', 'NOTICE', FALSE, TRUE, FALSE, 'ACTIVE', NULL, '198.51.100.16', DATE_SUB(@SEED_NOW, INTERVAL 8 DAY), @SEED_NOW),
    (970007, 930037, 920010, '크로키 모임 준비물 안내', 'A3 스케치북이나 아이패드 둘 다 괜찮습니다. 처음 오시는 분도 부담 없이 오실 수 있게 30분 워밍업 세션부터 진행할 예정입니다.', 'NOTICE', FALSE, TRUE, FALSE, 'ACTIVE', NULL, '198.51.100.17', DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (970008, 930040, 920008, 'Transformer 논문 읽기 자료 공유', '이번 주는 attention 구조와 scaling law 부분 중심으로 읽습니다. 발표 담당자는 슬라이드 6장 이내로 정리해주세요.', 'RESOURCE', FALSE, FALSE, TRUE, 'ACTIVE', NULL, '198.51.100.18', DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (970009, 930021, 920014, '프론트 포트폴리오 피드백 교환해요', 'React, Next.js 위주 포트폴리오면 더 좋고, 디자인 완성도보다 문제 해결 흐름이 잘 드러나는지 같이 보려고 합니다.', 'GENERAL', FALSE, FALSE, TRUE, 'ACTIVE', NULL, '198.51.100.19', DATE_SUB(@SEED_NOW, INTERVAL 6 DAY), @SEED_NOW),
    (970010, 930029, 920009, '상반기 인턴 자소서 같이 보실 분', '마케팅, PM, 데이터 직무 위주로 자소서와 이력서 상호 피드백 스터디를 열려고 합니다. 오프라인은 종각 근처 생각 중입니다.', 'RECRUITMENT', FALSE, FALSE, TRUE, 'ACTIVE', NULL, '198.51.100.20', DATE_SUB(@SEED_NOW, INTERVAL 6 DAY), @SEED_NOW),
    (970011, 930013, 920022, '학교 축제 영상 촬영 팀 구해요', '기획, 촬영, 색보정까지 같이 해볼 분을 찾고 있습니다. 행사 경험 없어도 편집 툴 익숙하면 충분합니다.', 'RECRUITMENT', FALSE, FALSE, TRUE, 'ACTIVE', NULL, '198.51.100.21', DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (970012, 930006, 920003, '임베디드 회로 스터디 자료 정리', 'STM32 기반으로 GPIO, UART, 인터럽트 흐름을 한 번에 정리했습니다. 회로도 캡처와 예제 코드도 같이 첨부합니다.', 'RESOURCE', FALSE, FALSE, TRUE, 'ACTIVE', NULL, '198.51.100.22', DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (970013, 930034, 920018, '다음 합주곡 후보 받습니다', '공연까지 한 달 정도 남아서 3곡 정도로 셋리스트 압축하려고 합니다. 난이도와 보컬 키도 같이 적어주세요.', 'GENERAL', FALSE, FALSE, FALSE, 'ACTIVE', NULL, '198.51.100.23', DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (970014, 930035, 920011, '여의도 10km 러닝 공지', '평일 저녁 8시에 한강공원 여의나루역 인근에서 시작합니다. 페이스는 6분대 초반으로 맞출 예정입니다.', 'NOTICE', FALSE, FALSE, FALSE, 'ACTIVE', NULL, '198.51.100.24', DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (970015, 930039, 920013, '부산 백엔드 스터디 2주차 정리', 'JPA 지연 로딩과 N+1 문제, fetch join 차이를 예제 코드 중심으로 정리했습니다. 실무에서 자주 부딪히는 케이스 위주입니다.', 'RESOURCE', FALSE, FALSE, FALSE, 'ACTIVE', NULL, '198.51.100.25', DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW),
    (970016, 930031, 920021, '영어 회화 번개 모집합니다', '주제는 유학 이야기보다 가볍게 일상, 취미 중심으로 가려고 합니다. 초급보다는 중급 정도가 편할 것 같습니다.', 'RECRUITMENT', FALSE, FALSE, TRUE, 'ACTIVE', NULL, '198.51.100.26', DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW),
    (970017, 930026, 920012, '아이패드 드로잉 브러시 추천해요', '선 정리용, 색감용, 텍스처용으로 실제 자주 쓰는 조합을 정리했습니다. 취향 따라 다르지만 입문자 기준으로 골랐습니다.', 'REVIEW', FALSE, FALSE, TRUE, 'ACTIVE', NULL, '198.51.100.27', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (970018, 930022, 920016, 'MLOps 세미나 같이 갈 사람', '다음 주에 코엑스에서 열리는 세미나 같이 갈 사람 있으면 댓글 주세요. 배포 경험이 없어도 들을 만한 세션이 꽤 있어 보입니다.', 'GENERAL', FALSE, FALSE, TRUE, 'ACTIVE', NULL, '198.51.100.28', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW);

INSERT INTO post_view_count (
    post_id,
    view_count,
    created_at,
    updated_at
) VALUES
    (970001, 148, DATE_SUB(@SEED_NOW, INTERVAL 12 DAY), @SEED_NOW),
    (970002, 121, DATE_SUB(@SEED_NOW, INTERVAL 11 DAY), @SEED_NOW),
    (970003, 176, DATE_SUB(@SEED_NOW, INTERVAL 10 DAY), @SEED_NOW),
    (970004, 132, DATE_SUB(@SEED_NOW, INTERVAL 9 DAY), @SEED_NOW),
    (970005, 89, DATE_SUB(@SEED_NOW, INTERVAL 8 DAY), @SEED_NOW),
    (970006, 74, DATE_SUB(@SEED_NOW, INTERVAL 8 DAY), @SEED_NOW),
    (970007, 67, DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (970008, 112, DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (970009, 118, DATE_SUB(@SEED_NOW, INTERVAL 6 DAY), @SEED_NOW),
    (970010, 96, DATE_SUB(@SEED_NOW, INTERVAL 6 DAY), @SEED_NOW),
    (970011, 83, DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (970012, 92, DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (970013, 54, DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (970014, 62, DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (970015, 71, DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW),
    (970016, 79, DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW),
    (970017, 103, DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (970018, 87, DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW);

INSERT INTO post_tag (
    post_id,
    tag_name
) VALUES
    (970001, 'spring'),
    (970001, 'study'),
    (970002, 'pm'),
    (970002, 'startup'),
    (970003, 'band'),
    (970003, 'practice-room'),
    (970004, 'futsal'),
    (970005, 'session'),
    (970006, 'sprint'),
    (970007, 'croquis'),
    (970008, 'paper-reading'),
    (970009, 'portfolio'),
    (970010, 'internship'),
    (970011, 'video'),
    (970012, 'embedded'),
    (970013, 'setlist'),
    (970014, 'running'),
    (970015, 'jpa'),
    (970016, 'english'),
    (970017, 'drawing'),
    (970018, 'mlops');

INSERT INTO post_image (
    post_id,
    image_url,
    display_order
) VALUES
    (970003, 'https://picsum.photos/seed/post-room/600/400', 0),
    (970007, 'https://picsum.photos/seed/post-supplies/600/400', 0),
    (970011, 'https://picsum.photos/seed/post-storyboard/600/400', 0),
    (970017, 'https://picsum.photos/seed/post-brushes/600/400', 0);

INSERT INTO comment (
    comment_id,
    post_id,
    member_id,
    parent_id,
    depth,
    content,
    is_anonymous,
    status,
    like_count,
    reply_count,
    deleted_at,
    client_ip,
    created_at,
    updated_at
) VALUES
    (980001, 970001, 920013, NULL, 0, '백엔드 포지션이면 Java/Spring 기준인가요?', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.101', DATE_SUB(@SEED_NOW, INTERVAL 11 DAY), @SEED_NOW),
    (980002, 970001, 920001, 980001, 1, '네, 이번 기수는 Spring Boot 기준으로 진행하려고 합니다.', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.102', DATE_SUB(@SEED_NOW, INTERVAL 11 DAY), @SEED_NOW),
    (980003, 970002, 920015, NULL, 0, '저도 PM/데이터 쪽 관심 있어서 참여하고 싶어요.', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.103', DATE_SUB(@SEED_NOW, INTERVAL 10 DAY), @SEED_NOW),
    (980004, 970003, 920018, NULL, 0, '합주실은 성수 쪽이 장비 상태가 제일 무난했습니다.', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.104', DATE_SUB(@SEED_NOW, INTERVAL 10 DAY), @SEED_NOW),
    (980005, 970004, 920007, NULL, 0, '저 포함하면 현재 7명 맞을까요?', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.105', DATE_SUB(@SEED_NOW, INTERVAL 9 DAY), @SEED_NOW),
    (980006, 970005, 920016, NULL, 0, '세션 자료 감사합니다. 이번 주는 인증 파트 먼저 보겠습니다.', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.106', DATE_SUB(@SEED_NOW, INTERVAL 8 DAY), @SEED_NOW),
    (980007, 970006, 920009, NULL, 0, '스프린트 회의 전까지 KPI 초안 정리해서 올려둘게요.', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.107', DATE_SUB(@SEED_NOW, INTERVAL 8 DAY), @SEED_NOW),
    (980008, 970007, 920012, NULL, 0, '4B 연필도 챙겨오면 명암 잡기 편합니다.', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.108', DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (980009, 970008, 920005, NULL, 0, '이번 주는 3장까지만 읽어도 괜찮을까요?', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.109', DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (980010, 970008, 920008, 980009, 1, '네, 대신 질문 하나씩 준비해오면 좋겠습니다.', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.110', DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (980011, 970009, 920010, NULL, 0, '포트폴리오 링크 있으면 같이 보면서 피드백 드릴게요.', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.111', DATE_SUB(@SEED_NOW, INTERVAL 6 DAY), @SEED_NOW),
    (980012, 970010, 920020, NULL, 0, '자소서 첨삭은 구글독스로 진행하나요?', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.112', DATE_SUB(@SEED_NOW, INTERVAL 6 DAY), @SEED_NOW),
    (980013, 970011, 920012, NULL, 0, '축제 촬영이면 색보정 담당도 같이 있으면 좋겠네요.', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.113', DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (980014, 970012, 920023, NULL, 0, 'STM32 기준 예제가 있으면 입문자도 보기 쉬울 것 같아요.', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.114', DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (980015, 970013, 920006, NULL, 0, '보컬 기준으로는 잔나비 곡도 괜찮을 것 같아요.', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.115', DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (980016, 970014, 920019, NULL, 0, '토요일 비 오면 실내 트랙으로 장소 변경하겠습니다.', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.116', DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (980017, 970015, 920001, NULL, 0, '정리 깔끔하네요. 다음 주는 JPA N+1도 같이 다뤄보죠.', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.117', DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW),
    (980018, 970016, 920021, NULL, 0, '중급 이상만 가능한지 궁금합니다.', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.118', DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW),
    (980019, 970017, 920004, NULL, 0, '프로크리에이트 기본 브러시도 충분히 좋더라고요.', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.119', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (980020, 970018, 920023, NULL, 0, '세미나면 쿠버네티스 경험 없어도 괜찮을까요?', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.120', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (980021, 970018, 920016, 980020, 1, '입문자도 괜찮아요. 운영 경험보다 개념 위주 세션이 많습니다.', FALSE, 'ACTIVE', 0, 0, NULL, '198.51.100.121', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW);

INSERT INTO comment_like (
    comment_id,
    member_id,
    created_at,
    updated_at
) VALUES
    (980001, 920023, DATE_SUB(@SEED_NOW, INTERVAL 10 DAY), @SEED_NOW),
    (980003, 920002, DATE_SUB(@SEED_NOW, INTERVAL 9 DAY), @SEED_NOW),
    (980004, 920006, DATE_SUB(@SEED_NOW, INTERVAL 9 DAY), @SEED_NOW),
    (980006, 920014, DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (980009, 920016, DATE_SUB(@SEED_NOW, INTERVAL 6 DAY), @SEED_NOW),
    (980011, 920014, DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (980013, 920022, DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (980017, 920013, DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (980019, 920012, DATE_SUB(@SEED_NOW, INTERVAL 1 DAY), @SEED_NOW),
    (980020, 920008, DATE_SUB(@SEED_NOW, INTERVAL 1 DAY), @SEED_NOW);

INSERT INTO post_like (
    post_id,
    member_id,
    created_at,
    updated_at
) VALUES
    (970001, 920013, DATE_SUB(@SEED_NOW, INTERVAL 11 DAY), @SEED_NOW),
    (970001, 920014, DATE_SUB(@SEED_NOW, INTERVAL 11 DAY), @SEED_NOW),
    (970001, 920023, DATE_SUB(@SEED_NOW, INTERVAL 10 DAY), @SEED_NOW),
    (970002, 920005, DATE_SUB(@SEED_NOW, INTERVAL 10 DAY), @SEED_NOW),
    (970002, 920015, DATE_SUB(@SEED_NOW, INTERVAL 10 DAY), @SEED_NOW),
    (970003, 920018, DATE_SUB(@SEED_NOW, INTERVAL 9 DAY), @SEED_NOW),
    (970003, 920012, DATE_SUB(@SEED_NOW, INTERVAL 9 DAY), @SEED_NOW),
    (970003, 920021, DATE_SUB(@SEED_NOW, INTERVAL 9 DAY), @SEED_NOW),
    (970004, 920007, DATE_SUB(@SEED_NOW, INTERVAL 8 DAY), @SEED_NOW),
    (970004, 920011, DATE_SUB(@SEED_NOW, INTERVAL 8 DAY), @SEED_NOW),
    (970005, 920001, DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (970005, 920016, DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (970006, 920009, DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (970006, 920010, DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (970007, 920012, DATE_SUB(@SEED_NOW, INTERVAL 6 DAY), @SEED_NOW),
    (970008, 920005, DATE_SUB(@SEED_NOW, INTERVAL 6 DAY), @SEED_NOW),
    (970008, 920023, DATE_SUB(@SEED_NOW, INTERVAL 6 DAY), @SEED_NOW),
    (970009, 920010, DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (970009, 920015, DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (970010, 920020, DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (970011, 920012, DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (970012, 920023, DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (970013, 920018, DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW),
    (970014, 920019, DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW),
    (970015, 920001, DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (970016, 920021, DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (970017, 920010, DATE_SUB(@SEED_NOW, INTERVAL 1 DAY), @SEED_NOW),
    (970017, 920022, DATE_SUB(@SEED_NOW, INTERVAL 1 DAY), @SEED_NOW),
    (970018, 920008, DATE_SUB(@SEED_NOW, INTERVAL 1 DAY), @SEED_NOW),
    (970018, 920023, DATE_SUB(@SEED_NOW, INTERVAL 1 DAY), @SEED_NOW);

INSERT INTO bookmark (
    member_id,
    post_id,
    created_at,
    updated_at
) VALUES
    (920014, 970001, DATE_SUB(@SEED_NOW, INTERVAL 10 DAY), @SEED_NOW),
    (920005, 970002, DATE_SUB(@SEED_NOW, INTERVAL 9 DAY), @SEED_NOW),
    (920021, 970003, DATE_SUB(@SEED_NOW, INTERVAL 9 DAY), @SEED_NOW),
    (920007, 970004, DATE_SUB(@SEED_NOW, INTERVAL 8 DAY), @SEED_NOW),
    (920016, 970005, DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (920010, 970006, DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (920022, 970011, DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (920023, 970012, DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (920020, 970010, DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (920008, 970018, DATE_SUB(@SEED_NOW, INTERVAL 1 DAY), @SEED_NOW);

INSERT INTO post_like_count (
    post_id,
    like_count,
    created_at,
    updated_at
)
SELECT
    post_id,
    COUNT(*) AS like_count,
    @SEED_NOW,
    @SEED_NOW
FROM post_like
WHERE post_id BETWEEN @POST_START_ID AND @POST_END_ID
GROUP BY post_id;

INSERT INTO post_comment_count (
    post_id,
    comment_count,
    created_at,
    updated_at
)
SELECT
    post_id,
    COUNT(*) AS comment_count,
    @SEED_NOW,
    @SEED_NOW
FROM comment
WHERE post_id BETWEEN @POST_START_ID AND @POST_END_ID
GROUP BY post_id;

INSERT INTO post_bookmark_count (
    post_id,
    bookmark_count,
    created_at,
    updated_at
)
SELECT
    post_id,
    COUNT(*) AS bookmark_count,
    @SEED_NOW,
    @SEED_NOW
FROM bookmark
WHERE post_id BETWEEN @POST_START_ID AND @POST_END_ID
GROUP BY post_id;

UPDATE comment c
JOIN (
    SELECT comment_id, COUNT(*) AS like_count
    FROM comment_like
    WHERE comment_id BETWEEN @COMMENT_START_ID AND @COMMENT_END_ID
    GROUP BY comment_id
) cl
  ON cl.comment_id = c.comment_id
SET c.like_count = cl.like_count
WHERE c.comment_id BETWEEN @COMMENT_START_ID AND @COMMENT_END_ID;

UPDATE comment c
JOIN (
    SELECT parent_id, COUNT(*) AS reply_count
    FROM comment
    WHERE parent_id IS NOT NULL
      AND comment_id BETWEEN @COMMENT_START_ID AND @COMMENT_END_ID
    GROUP BY parent_id
) cr
  ON cr.parent_id = c.comment_id
SET c.reply_count = cr.reply_count
WHERE c.comment_id BETWEEN @COMMENT_START_ID AND @COMMENT_END_ID;

INSERT INTO recruitment (
    recruitment_id,
    group_id,
    author_id,
    title,
    description,
    positions,
    requirements,
    duration,
    goal,
    process_description,
    deadline,
    status,
    application_count,
    deleted_at,
    created_at,
    updated_at
) VALUES
    (962001, 960001, 920001, '서울대 개발자 라운지 2026 봄 신규 멤버 모집', '백엔드와 프론트엔드를 함께 만드는 대학연합 프로젝트 팀입니다. 8주 동안 작은 기능을 직접 배포하는 것을 목표로 합니다.', JSON_ARRAY(JSON_OBJECT('name', '백엔드', 'count', 2), JSON_OBJECT('name', '프론트엔드', 'count', 1)), '주 1회 세션 참여 가능, 기본 Git 사용 경험, 맡은 역할에 대한 책임감', '2026년 3월 ~ 6월', '실사용 가능한 MVP 1개 완성', '서류 확인 후 간단한 온라인 인터뷰로 진행합니다.', '2026-03-31 23:59:59', 'OPEN', 3, NULL, DATE_SUB(@SEED_NOW, INTERVAL 9 DAY), @SEED_NOW),
    (962002, 960002, 920006, '안암 밴드클럽 객원 키보드/베이스 모집', '학기말 공연을 앞두고 객원 멤버를 모집합니다. 정기 합주 참여 가능하면 전공과 학교는 상관없습니다.', JSON_ARRAY(JSON_OBJECT('name', '키보드', 'count', 1), JSON_OBJECT('name', '베이스', 'count', 1)), '주 1회 합주 참석 가능, 기본 장비 보유 또는 대여 가능, 공연 리허설 참석 가능', '2026년 3월 ~ 5월', '학기말 공연 1회와 교내 버스킹 1회 진행', '지원서 확인 후 합주 한 번으로 최종 결정합니다.', '2026-03-25 23:59:59', 'OPEN', 2, NULL, DATE_SUB(@SEED_NOW, INTERVAL 8 DAY), @SEED_NOW),
    (962003, 960008, 920008, 'AI 논문 읽기 모임 발표 담당 모집', '매주 논문 요약 발표를 맡아줄 멤버를 찾고 있습니다. 최신 모델보다 읽은 내용을 명확하게 정리하는 분을 선호합니다.', JSON_ARRAY(JSON_OBJECT('name', '발표 담당', 'count', 2)), '주 1회 온라인 참석 가능, 논문 요약 슬라이드 작성 가능', '2026년 3월 ~ 4월', '한 달 동안 4편의 논문 발표 완주', '지원서 확인 후 이전 발표 경험이나 정리 방식 위주로 간단히 이야기합니다.', '2026-03-28 23:59:59', 'OPEN', 1, NULL, DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW);

INSERT INTO form_item (
    form_item_id,
    recruitment_id,
    question,
    question_type,
    is_required,
    options,
    display_order,
    created_at,
    updated_at
) VALUES
    (963001, 962001, '지원 동기를 알려주세요.', 'TEXT', TRUE, NULL, 1, DATE_SUB(@SEED_NOW, INTERVAL 9 DAY), @SEED_NOW),
    (963002, 962001, '주당 투입 가능한 시간을 골라주세요.', 'SELECT', TRUE, JSON_ARRAY('3시간 이하', '3~5시간', '5시간 이상'), 2, DATE_SUB(@SEED_NOW, INTERVAL 9 DAY), @SEED_NOW),
    (963003, 962001, '최근 만든 프로젝트 링크가 있으면 적어주세요.', 'TEXT', FALSE, NULL, 3, DATE_SUB(@SEED_NOW, INTERVAL 9 DAY), @SEED_NOW),
    (963004, 962002, '선호 장르와 자주 연주하는 곡을 적어주세요.', 'TEXT', TRUE, NULL, 1, DATE_SUB(@SEED_NOW, INTERVAL 8 DAY), @SEED_NOW),
    (963005, 962002, '공연 경험을 골라주세요.', 'SELECT', TRUE, JSON_ARRAY('없음', '교내 공연 1~2회', '정기 공연 경험 다수'), 2, DATE_SUB(@SEED_NOW, INTERVAL 8 DAY), @SEED_NOW),
    (963006, 962002, '합주 가능 요일을 골라주세요.', 'MULTI_SELECT', TRUE, JSON_ARRAY('월요일 저녁', '수요일 저녁', '금요일 저녁', '토요일 오후'), 3, DATE_SUB(@SEED_NOW, INTERVAL 8 DAY), @SEED_NOW),
    (963007, 962003, '최근 읽은 논문이나 관심 있는 주제를 알려주세요.', 'TEXT', TRUE, NULL, 1, DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (963008, 962003, '발표 경험을 골라주세요.', 'SELECT', TRUE, JSON_ARRAY('없음', '스터디 발표 1~2회', '세미나 발표 다수'), 2, DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW),
    (963009, 962003, '사용 가능한 실험 환경이나 툴이 있으면 적어주세요.', 'TEXT', FALSE, NULL, 3, DATE_SUB(@SEED_NOW, INTERVAL 7 DAY), @SEED_NOW);

INSERT INTO recruitment_application (
    recruitment_application_id,
    recruitment_id,
    applicant_id,
    position,
    portfolio_url,
    status,
    reviewed_by,
    reviewed_at,
    created_at,
    updated_at
) VALUES
    (964001, 962001, 920013, '백엔드', 'https://github.com/gaeul-backend-demo', 'IN_REVIEW', 920014, DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), DATE_SUB(@SEED_NOW, INTERVAL 6 DAY), @SEED_NOW),
    (964002, 962001, 920016, '프론트엔드', 'https://github.com/siwoo-ai-demo', 'SUBMITTED', NULL, NULL, DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (964003, 962001, 920023, '백엔드', 'https://github.com/jiho-robot-demo', 'APPROVED', 920001, DATE_SUB(@SEED_NOW, INTERVAL 1 DAY), DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (964004, 962002, 920021, '키보드', NULL, 'SUBMITTED', NULL, NULL, DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (964005, 962002, 920022, '베이스', 'https://picsum.photos/seed/portfolio-hangyeol/800/600', 'IN_REVIEW', 920018, DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (964006, 962003, 920009, '발표 담당', 'https://www.notion.so/cluverse/chaewon-brand', 'APPROVED', 920008, DATE_SUB(@SEED_NOW, INTERVAL 1 DAY), DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW);

INSERT INTO application_status_history (
    recruitment_application_id,
    previous_status,
    new_status,
    changed_by,
    note,
    source_system,
    client_ip,
    created_at,
    updated_at
) VALUES
    (964001, 'SUBMITTED', 'IN_REVIEW', 920014, '프로젝트 경험 확인 중', 'WEB_USER', '203.0.113.24', DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW),
    (964003, 'SUBMITTED', 'IN_REVIEW', 920014, '기술 스택 확인 완료', 'WEB_USER', '203.0.113.24', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (964003, 'IN_REVIEW', 'APPROVED', 920001, '백엔드 포지션 합류 확정', 'WEB_USER', '203.0.113.11', DATE_SUB(@SEED_NOW, INTERVAL 1 DAY), @SEED_NOW),
    (964005, 'SUBMITTED', 'IN_REVIEW', 920018, '합주 가능한 일정 확인 중', 'WEB_USER', '203.0.113.28', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (964006, 'SUBMITTED', 'IN_REVIEW', 920016, '발표 경험 확인 중', 'WEB_USER', '203.0.113.26', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (964006, 'IN_REVIEW', 'APPROVED', 920008, '발표 담당으로 합류 확정', 'WEB_USER', '203.0.113.18', DATE_SUB(@SEED_NOW, INTERVAL 1 DAY), @SEED_NOW);

INSERT INTO form_item_answer (
    recruitment_application_id,
    form_item_id,
    answer,
    created_at,
    updated_at
) VALUES
    (964001, 963001, '실무형 백엔드 스터디를 찾고 있었고, JPA와 테스트 코드를 더 탄탄하게 다지고 싶습니다.', DATE_SUB(@SEED_NOW, INTERVAL 6 DAY), @SEED_NOW),
    (964001, 963002, '5시간 이상', DATE_SUB(@SEED_NOW, INTERVAL 6 DAY), @SEED_NOW),
    (964001, 963003, 'https://github.com/gaeul-backend-demo/order-api', DATE_SUB(@SEED_NOW, INTERVAL 6 DAY), @SEED_NOW),
    (964002, 963001, 'AI 전공이지만 프론트 구현 경험도 쌓아보고 싶어서 지원했습니다.', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (964002, 963002, '3~5시간', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (964002, 963003, 'https://github.com/siwoo-ai-demo/dashboard-prototype', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (964003, 963001, '백엔드 아키텍처와 배포 경험을 같이 쌓고 싶습니다.', DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (964003, 963002, '5시간 이상', DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (964003, 963003, 'https://github.com/jiho-robot-demo/robot-platform', DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (964004, 963004, '브릿팝과 국내 인디 계열 곡을 자주 연주합니다.', DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (964004, 963005, '교내 공연 1~2회', DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (964004, 963006, '["수요일 저녁","토요일 오후"]', DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (964005, 963004, '록과 시티팝 계열을 좋아하고 베이스 라인 정리하는 걸 즐깁니다.', DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (964005, 963005, '교내 공연 1~2회', DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (964005, 963006, '["금요일 저녁","토요일 오후"]', DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (964006, 963007, 'LLM 평가와 실험 추적 방식에 관심이 있습니다.', DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW),
    (964006, 963008, '스터디 발표 1~2회', DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW),
    (964006, 963009, 'Colab, Notion, Figma를 주로 사용합니다.', DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW);

INSERT INTO application_chat_message (
    recruitment_application_id,
    sender_id,
    content,
    is_read,
    deleted_at,
    client_ip,
    created_at,
    updated_at
) VALUES
    (964001, 920014, '지원서 잘 봤습니다. 이번 주 중으로 짧게 이야기 가능하실까요?', TRUE, NULL, '203.0.113.24', DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW),
    (964001, 920013, '네 가능합니다. 평일 저녁 시간대면 편합니다.', TRUE, NULL, '203.0.113.23', DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW),
    (964005, 920018, '베이스 파트 경험이 어느 정도인지 궁금합니다.', FALSE, NULL, '203.0.113.28', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (964005, 920022, '학교 축제 밴드에서 1년 정도 연주했습니다.', FALSE, NULL, '203.0.113.32', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (964006, 920008, '발표 자료 정리 스타일이 좋아서 이번 주부터 바로 참여 가능하실 것 같습니다.', TRUE, NULL, '203.0.113.18', DATE_SUB(@SEED_NOW, INTERVAL 1 DAY), @SEED_NOW),
    (964006, 920009, '확인했습니다. 첫 발표는 다음 주 논문으로 준비해보겠습니다.', TRUE, NULL, '203.0.113.19', DATE_SUB(@SEED_NOW, INTERVAL 1 DAY), @SEED_NOW);

INSERT INTO calendar_item (
    group_id,
    member_id,
    title,
    description,
    start_at,
    end_at,
    is_all_day,
    location,
    deleted_at,
    created_at,
    updated_at
) VALUES
    (960001, 920001, 'Spring Boot 코드리뷰 세션', '인증/인가 파트 코드리뷰와 PR 리뷰 규칙 정리', '2026-03-20 19:30:00', '2026-03-20 21:30:00', FALSE, '서울대 301동 라운지', NULL, DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (960002, 920006, '안암 밴드클럽 정기 합주', '학기말 공연 후보곡 3곡 리허설', '2026-03-21 18:30:00', '2026-03-21 21:00:00', FALSE, '안암 합주실 B', NULL, DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (960003, 920011, '여의도 야간 러닝', '10km 여의도 코스', '2026-03-22 20:00:00', '2026-03-22 21:20:00', FALSE, '여의나루역 2번 출구', NULL, DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (960004, 920002, '프로덕트 랩 인터뷰 정리 워크숍', '인터뷰 결과 구조화와 JTBD 정리', '2026-03-23 19:00:00', '2026-03-23 21:00:00', FALSE, '성수 스터디룸 4', NULL, DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (960006, 920019, '주말 풋살 매치', '4:4 자체전 후 포지션 로테이션', '2026-03-22 09:00:00', '2026-03-22 11:00:00', FALSE, '잠실 풋살장 C코트', NULL, DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (960008, 920008, '논문 읽기 정기 세션', 'Transformer 구조 요약 발표', '2026-03-24 21:00:00', '2026-03-24 22:30:00', FALSE, 'Discord Online', NULL, DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW);

INSERT INTO calendar_event (
    calendar_event_id,
    member_id,
    title,
    description,
    category,
    start_at,
    end_at,
    location,
    all_day,
    visibility,
    created_at,
    updated_at
) VALUES
    (992001, 920001, '서울대 개발자 라운지 코드리뷰', '백엔드/프론트 합동 코드리뷰와 다음 스프린트 이슈 정리', 'GROUP', '2026-03-20 19:30:00', '2026-03-20 21:30:00', '서울대 301동 라운지', FALSE, 'MEMBERS', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (992002, 920002, '사용자 인터뷰 리캡 작성', '이번 주 인터뷰 4건 요약과 JTBD 초안 정리', 'PERSONAL', '2026-03-21 14:00:00', '2026-03-21 16:00:00', '연세대 중앙도서관 스터디룸', FALSE, 'PRIVATE', DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW),
    (992003, 920006, '안암 밴드클럽 정기 합주', '학기말 공연 후보곡 3곡 리허설', 'GROUP', '2026-03-21 18:30:00', '2026-03-21 21:00:00', '안암 합주실 B', FALSE, 'MEMBERS', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (992004, 920011, '한강 러닝 10km', '여의도 코스 야간 러닝', 'GROUP', '2026-03-22 20:00:00', '2026-03-22 21:20:00', '여의나루역 2번 출구', FALSE, 'PUBLIC', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (992005, 920016, 'MLOps 세미나 참석', '코엑스 세미나 참석 후 메모 정리 예정', 'SCHOOL', '2026-03-26 10:00:00', '2026-03-26 17:30:00', '코엑스 컨퍼런스룸 E', FALSE, 'PUBLIC', DATE_SUB(@SEED_NOW, INTERVAL 1 DAY), @SEED_NOW),
    (992006, 920024, '운영 정책 점검', '신고 처리 기준과 알림 노출 정책 검토', 'PERSONAL', '2026-03-24 09:30:00', '2026-03-24 11:00:00', '클루버스 운영 콘솔', FALSE, 'PRIVATE', DATE_SUB(@SEED_NOW, INTERVAL 1 DAY), @SEED_NOW),
    (992007, 920019, '잠실 풋살 정기전', '주전/교체 인원 섞어서 4:4 자체전 진행', 'GROUP', '2026-03-22 09:00:00', '2026-03-22 11:00:00', '잠실 풋살장 C코트', FALSE, 'MEMBERS', DATE_SUB(@SEED_NOW, INTERVAL 2 DAY), @SEED_NOW),
    (992008, 920024, '봄 학기 인증 점검일', '학교 이메일 인증 적체 여부와 반려 사유 템플릿 점검', 'SCHOOL', '2026-03-25 00:00:00', '2026-03-25 23:59:00', '운영센터', TRUE, 'PUBLIC', DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW);

INSERT INTO campus_event (
    campus_event_id,
    title,
    host,
    start_date,
    end_date,
    location,
    thumbnail_image_url,
    summary,
    created_at,
    updated_at
) VALUES
    (993001, '서울권 대학연합 해커톤 설명회', '클루버스 X 서울대 개발자 라운지', '2026-03-27', '2026-03-27', '서울대학교 해동학술문화관', 'https://picsum.photos/seed/campus-event-993001/1200/800', '해커톤 일정, 팀 빌딩 방식, 지원 트랙을 소개하는 오프라인 설명회입니다.', DATE_SUB(@SEED_NOW, INTERVAL 6 DAY), @SEED_NOW),
    (993002, '연세 창업 아이디어 피칭 나이트', '연세대학교 창업지원단', '2026-03-28', '2026-03-28', '연세대학교 백양누리', 'https://picsum.photos/seed/campus-event-993002/1200/800', '예비 창업팀과 PM, 디자이너, 개발자가 함께 참여하는 아이디어 피칭 행사입니다.', DATE_SUB(@SEED_NOW, INTERVAL 5 DAY), @SEED_NOW),
    (993003, '홍대 봄 축제 미디어 아트 전시', '홍익대학교 미디어커뮤니케이션학과', '2026-04-02', '2026-04-05', '홍문관 갤러리홀', 'https://picsum.photos/seed/campus-event-993003/1200/800', '학생 영상/미디어아트 작품과 라이브 세션이 함께 열리는 봄 학기 전시 행사입니다.', DATE_SUB(@SEED_NOW, INTERVAL 4 DAY), @SEED_NOW),
    (993004, '대학연합 러닝 크루 오픈런', '한강 러닝 메이트', '2026-03-30', '2026-03-30', '여의도 한강공원 이벤트 광장', 'https://picsum.photos/seed/campus-event-993004/1200/800', '서울권 대학생 러너들이 함께하는 오픈런 행사로, 초급/중급 페이스 그룹이 나뉘어 운영됩니다.', DATE_SUB(@SEED_NOW, INTERVAL 3 DAY), @SEED_NOW);

ALTER TABLE university AUTO_INCREMENT = 91013;
ALTER TABLE member AUTO_INCREMENT = 920025;
ALTER TABLE board AUTO_INCREMENT = 930041;
ALTER TABLE major AUTO_INCREMENT = 940014;
ALTER TABLE interest AUTO_INCREMENT = 950020;
ALTER TABLE `group` AUTO_INCREMENT = 960009;
ALTER TABLE group_role AUTO_INCREMENT = 961009;
ALTER TABLE recruitment AUTO_INCREMENT = 962004;
ALTER TABLE form_item AUTO_INCREMENT = 963010;
ALTER TABLE recruitment_application AUTO_INCREMENT = 964007;
ALTER TABLE post AUTO_INCREMENT = 970019;
ALTER TABLE comment AUTO_INCREMENT = 980022;
ALTER TABLE notification AUTO_INCREMENT = 990009;
ALTER TABLE member_report AUTO_INCREMENT = 991004;
ALTER TABLE calendar_event AUTO_INCREMENT = 992009;
ALTER TABLE campus_event AUTO_INCREMENT = 993005;
