package cluverse.member.service;

import cluverse.member.service.implement.MemberReader;
import cluverse.member.service.implement.MemberWriter;
import cluverse.member.service.request.AddInterestRequest;
import cluverse.member.service.response.BlockedMemberResponse;
import cluverse.member.service.response.MemberInterestResponse;
import cluverse.member.service.response.MemberProfileResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberReader memberReader;

    @Mock
    private MemberWriter memberWriter;

    @InjectMocks
    private MemberService memberService;

    @Test
    void 프로필_조회는_reader에_위임한다() {
        MemberProfileResponse response = new MemberProfileResponse(
                1L, "luna", null, null, null, null, null, null, null, null,
                null, true, List.of(), false, false, 0L, 0L
        );
        when(memberReader.getProfile(1L, 2L)).thenReturn(response);

        MemberProfileResponse result = memberService.getProfile(1L, 2L);

        assertThat(result).isSameAs(response);
        verify(memberReader).getProfile(1L, 2L);
    }

    @Test
    void 차단_목록_조회는_reader에_위임한다() {
        List<BlockedMemberResponse> responses = List.of(
                new BlockedMemberResponse(2L, "blocked-user", 30L, "클루대", "badge", "profile", null)
        );
        when(memberReader.getBlockedMembers(1L)).thenReturn(responses);

        List<BlockedMemberResponse> result = memberService.getBlockedMembers(1L);

        assertThat(result).isEqualTo(responses);
        verify(memberReader).getBlockedMembers(1L);
    }

    @Test
    void 관심사_추가는_writer에_위임한다() {
        MemberInterestResponse response = new MemberInterestResponse(300L);
        when(memberWriter.addInterest(1L, new AddInterestRequest(300L))).thenReturn(response);

        MemberInterestResponse result = memberService.addInterest(1L, new AddInterestRequest(300L));

        assertThat(result).isEqualTo(response);
        verify(memberWriter).addInterest(1L, new AddInterestRequest(300L));
    }
}
