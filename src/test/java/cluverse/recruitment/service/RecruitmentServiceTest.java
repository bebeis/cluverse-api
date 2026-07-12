package cluverse.recruitment.service;

import cluverse.member.domain.Member;
import cluverse.member.service.implement.MemberReader;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.service.implement.RecruitmentReader;
import cluverse.recruitment.service.implement.RecruitmentWriter;
import cluverse.recruitment.service.request.RecruitmentCreateRequest;
import cluverse.recruitment.service.request.RecruitmentUpdateRequest;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecruitmentServiceTest {

    @Mock
    private RecruitmentReader recruitmentReader;

    @Mock
    private RecruitmentWriter recruitmentWriter;

    @Mock
    private MemberReader memberReader;

    @InjectMocks
    private RecruitmentService recruitmentService;

    @Test
    void 모집글_생성시_상세_응답을_반환한다() {
        // given
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

        when(recruitmentWriter.create(100L, 1L, request)).thenReturn(recruitment);
        when(recruitmentReader.readOrThrow(10L)).thenReturn(recruitment);
        when(memberReader.readMemberMap(List.of(100L))).thenReturn(Map.of(100L, author));

        // when
        RecruitmentDetailResponse result = recruitmentService.createRecruitment(100L, 1L, request);

        // then
        assertThat(result.recruitmentId()).isEqualTo(10L);
        assertThat(result.authorNickname()).isEqualTo("luna");
        verify(recruitmentWriter).create(100L, 1L, request);
    }

    @Test
    void 모집글_수정시_상세_응답을_반환한다() {
        // given
        RecruitmentUpdateRequest request = new RecruitmentUpdateRequest(
                "백엔드 모집 수정",
                "설명 수정",
                List.of(),
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 4, 30, 23, 59),
                List.of()
        );
        Recruitment recruitment = createRecruitment(10L, 1L, 100L);
        Member author = createMember(100L, "luna");

        when(recruitmentWriter.update(100L, 10L, request)).thenReturn(recruitment);
        when(memberReader.readMemberMap(List.of(100L))).thenReturn(Map.of(100L, author));

        // when
        RecruitmentDetailResponse result = recruitmentService.updateRecruitment(100L, 10L, request);

        // then
        assertThat(result.recruitmentId()).isEqualTo(10L);
        verify(recruitmentWriter).update(100L, 10L, request);
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
