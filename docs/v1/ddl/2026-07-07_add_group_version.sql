-- 2026-07-07 그룹 낙관적 락 버전 컬럼 추가
-- 목적: 정원(max_members) 검증·member_count 갱신이 동시 승인/가입 요청에서
--       lost update 없이 에그리거트 단위로 직렬화되도록 @Version 낙관적 락 도입

ALTER TABLE `group`
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전' AFTER member_count;
