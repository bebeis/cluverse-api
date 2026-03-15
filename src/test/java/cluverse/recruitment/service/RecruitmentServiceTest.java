package cluverse.recruitment.service;

import cluverse.common.exception.ForbiddenException;
import cluverse.group.domain.Group;
import cluverse.group.domain.GroupActivityType;
import cluverse.group.domain.GroupCategory;
import cluverse.group.domain.GroupVisibility;
import cluverse.member.domain.Member;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.service.implement.RecruitmentReader;
import cluverse.recruitment.service.implement.RecruitmentWriter;
import cluverse.recruitment.service.request.RecruitmentCreateRequest;
import cluverse.recruitment.service.response.RecruitmentDetailResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecruitmentServiceTest {

    @Mock
    private RecruitmentReader recruitmentReader;

    @Mock
    private RecruitmentWriter recruitmentWriter;

    @InjectMocks
    private RecruitmentService recruitmentService;

    @Test
    void 모집글_생성시_상세_응답을_반환한다() {
        // given
        Group group = createGroup(1L, 100L);
        RecruitmentCreateRequest request = new RecruitmentCreateRequest(
                "백엔드 모집",
                "스프링 백엔드 모집 공고",
                List.of(),
                "Spring Boot 경험",
                "3개월",
                "MVP 출시",
                "주 2회 온라인 회의",
                LocalDateTime.of(2026, 3, 31, 23, 59),
                List.of()
        );
        Recruitment recruitment = createRecruitment(10L, 1L, 100L);
        Member author = createMember(100L, "luna");

        when(recruitmentReader.readGroupOrThrow(1L)).thenReturn(group);
        when(recruitmentWriter.create(100L, 1L, request)).thenReturn(recruitment);
        when(recruitmentReader.readOrThrow(10L)).thenReturn(recruitment);
        when(recruitmentReader.readMemberMap(List.of(100L))).thenReturn(Map.of(100L, author));

        // when
        RecruitmentDetailResponse result = recruitmentService.createRecruitment(100L, 1L, request);

        // then
        assertThat(result.recruitmentId()).isEqualTo(10L);
        assertThat(result.authorNickname()).isEqualTo("luna");
        verify(recruitmentWriter).create(100L, 1L, request);
    }

    @Test
    void 모집글_생성은_그룹_매니저만_가능하다() {
        // given
        Group group = createGroup(1L, 100L);
        RecruitmentCreateRequest request = new RecruitmentCreateRequest(
                "백엔드 모집",
                "설명",
                List.of(),
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
        when(recruitmentReader.readGroupOrThrow(1L)).thenReturn(group);

        // when, then
        assertThatThrownBy(() -> recruitmentService.createRecruitment(200L, 1L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("모집글 관리 권한이 없습니다.");
    }

    private Group createGroup(Long groupId, Long ownerId) {
        Group group = Group.create(
                11L,
                "AI 프로젝트",
                "설명",
                null,
                GroupCategory.PROJECT,
                GroupActivityType.HYBRID,
                "서울",
                GroupVisibility.PUBLIC,
                ownerId,
                10,
                List.of()
        );
        ReflectionTestUtils.setField(group, "id", groupId);
        return group;
    }

    private Recruitment createRecruitment(Long recruitmentId, Long groupId, Long authorId) {
        Recruitment recruitment = Recruitment.create(
                groupId,
                authorId,
                "백엔드 모집",
                "설명",
                List.of(),
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 3, 31, 23, 59),
                List.of()
        );
        ReflectionTestUtils.setField(recruitment, "id", recruitmentId);
        return recruitment;
    }

    private Member createMember(Long memberId, String nickname) {
        Member member = Member.createSocialMember(nickname);
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }
}
