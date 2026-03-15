package cluverse.recruitment.service;

import cluverse.common.exception.BadRequestException;
import cluverse.group.domain.Group;
import cluverse.group.domain.GroupActivityType;
import cluverse.group.domain.GroupCategory;
import cluverse.group.domain.GroupVisibility;
import cluverse.member.domain.Member;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.domain.RecruitmentApplication;
import cluverse.recruitment.domain.RecruitmentApplicationStatus;
import cluverse.recruitment.service.implement.RecruitmentApplicationReader;
import cluverse.recruitment.service.implement.RecruitmentApplicationWriter;
import cluverse.recruitment.service.request.RecruitmentApplicationCreateRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationStatusUpdateRequest;
import cluverse.recruitment.service.response.RecruitmentApplicationDetailResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecruitmentApplicationServiceTest {

    @Mock
    private RecruitmentApplicationReader recruitmentApplicationReader;

    @Mock
    private RecruitmentApplicationWriter recruitmentApplicationWriter;

    @InjectMocks
    private RecruitmentApplicationService recruitmentApplicationService;

    @Test
    void 이미_지원한_모집글에는_다시_지원할_수_없다() {
        // given
        Recruitment recruitment = createRecruitment(10L, 1L, 100L);
        RecruitmentApplicationCreateRequest request = new RecruitmentApplicationCreateRequest(
                "Backend",
                null,
                List.of()
        );
        when(recruitmentApplicationReader.readRecruitmentOrThrow(10L)).thenReturn(recruitment);
        when(recruitmentApplicationReader.existsByRecruitmentAndApplicant(10L, 200L)).thenReturn(true);

        // when, then
        assertThatThrownBy(() -> recruitmentApplicationService.createApplication(200L, 10L, request, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("이미 지원한 모집글입니다.");
    }

    @Test
    void 지원_승인시_그룹_멤버로_편입된다() {
        // given
        Group group = createGroup(1L, 100L);
        Recruitment recruitment = createRecruitment(10L, 1L, 100L);
        RecruitmentApplication application = createApplication(30L, 10L, 200L);
        RecruitmentApplicationStatusUpdateRequest request = new RecruitmentApplicationStatusUpdateRequest(
                RecruitmentApplicationStatus.APPROVED,
                "합류 승인"
        );
        Member applicant = createMember(200L, "applicant");
        Member manager = createMember(100L, "manager");

        when(recruitmentApplicationReader.readOrThrow(30L)).thenReturn(application);
        when(recruitmentApplicationReader.readRecruitmentOrThrow(10L)).thenReturn(recruitment);
        when(recruitmentApplicationReader.readGroupOrThrow(1L)).thenReturn(group);
        when(recruitmentApplicationReader.readRecruitmentMap(List.of(10L))).thenReturn(Map.of(10L, recruitment));
        when(recruitmentApplicationReader.readMemberMap(List.of(200L, 100L))).thenReturn(Map.of(
                200L, applicant,
                100L, manager
        ));
        doAnswer(invocation -> {
            application.changeStatus(RecruitmentApplicationStatus.APPROVED, 100L, "합류 승인", "127.0.0.1");
            return null;
        }).when(recruitmentApplicationWriter).updateStatus(any(), any(), any(), any());

        // when
        RecruitmentApplicationDetailResponse result = recruitmentApplicationService.updateApplicationStatus(
                100L,
                10L,
                30L,
                request,
                "127.0.0.1"
        );

        // then
        assertThat(result.status()).isEqualTo(RecruitmentApplicationStatus.APPROVED);
        assertThat(group.hasMember(200L)).isTrue();
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

    private RecruitmentApplication createApplication(Long applicationId, Long recruitmentId, Long applicantId) {
        RecruitmentApplication application = RecruitmentApplication.create(
                recruitmentId,
                applicantId,
                "Backend",
                null,
                List.of(),
                "127.0.0.1"
        );
        ReflectionTestUtils.setField(application, "id", applicationId);
        return application;
    }

    private Member createMember(Long memberId, String nickname) {
        Member member = Member.createSocialMember(nickname);
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }
}
