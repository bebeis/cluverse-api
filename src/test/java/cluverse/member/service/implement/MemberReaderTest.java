package cluverse.member.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.member.domain.Member;
import cluverse.member.repository.BlockRepository;
import cluverse.member.repository.FollowRepository;
import cluverse.member.repository.MemberQueryRepository;
import cluverse.member.repository.MemberRepository;
import cluverse.member.service.response.BlockedMemberResponse;
import cluverse.member.service.response.MemberInterestResponse;
import cluverse.member.service.response.MemberProfileSummaryResponse;
import cluverse.university.domain.University;
import cluverse.university.repository.UniversityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberReaderTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberQueryRepository memberQueryRepository;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private UniversityRepository universityRepository;

    @InjectMocks
    private MemberReader memberReader;

    @Test
    void 차단_목록을_조회할_수_있다() {
        LocalDateTime blockedAt = LocalDateTime.of(2026, 3, 12, 10, 0);
        when(memberQueryRepository.findBlockedMembersByBlockerId(1L)).thenReturn(List.of(
                new MemberQueryRepository.BlockedMemberDTO(
                        2L,
                        "blocked-user",
                        30L,
                        "클루대",
                        "https://cdn.example.com/badge.png",
                        "https://cdn.example.com/profile.png",
                        blockedAt
                )
        ));

        List<BlockedMemberResponse> responses = memberReader.readBlockedMembers(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().memberId()).isEqualTo(2L);
        assertThat(responses.getFirst().nickname()).isEqualTo("blocked-user");
        assertThat(responses.getFirst().blockedAt()).isEqualTo(blockedAt);
    }

    @Test
    void 관심사_목록을_조회할_수_있다() {
        Member member = createMember(1L, "luna", 10L);
        member.addInterest(100L);
        member.addInterest(200L);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberQueryRepository.findInterestDetailsByInterestIds(List.of(100L, 200L))).thenReturn(List.of(
                new MemberQueryRepository.MemberInterestDetailDto(100L, "해커톤", "TECH"),
                new MemberQueryRepository.MemberInterestDetailDto(200L, "축제", "CAMPUS")
        ));

        List<MemberInterestResponse> responses = memberReader.readInterests(1L);

        assertThat(responses).containsExactly(
                new MemberInterestResponse(100L, "해커톤", "TECH"),
                new MemberInterestResponse(200L, "축제", "CAMPUS")
        );
    }

    @Test
    void 학교가_없으면_학교_요약_조회에_실패한다() {
        when(universityRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberReader.readUniversitySummary(10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 학교입니다.");
    }

    @Test
    void 학교_id가_null이면_학교_요약은_null이다() {
        MemberProfileSummaryResponse response = memberReader.readUniversitySummary(null);

        assertThat(response).isNull();
    }

    @Test
    void 회원을_읽을_수_있다() {
        Member member = createMember(1L, "luna", 10L);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        Member result = memberReader.readOrThrow(1L);

        assertThat(result).isSameAs(member);
    }

    private Member createMember(Long memberId, String nickname, Long universityId) {
        Member member = Member.create(nickname, universityId);
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }

    private University createUniversity(Long universityId, String name, String badgeImageUrl) {
        University university = mock(University.class);
        when(university.getId()).thenReturn(universityId);
        when(university.getName()).thenReturn(name);
        when(university.getBadgeImageUrl()).thenReturn(badgeImageUrl);
        return university;
    }
}
