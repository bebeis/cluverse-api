package cluverse.member.service;

import cluverse.member.domain.VerificationStatus;
import cluverse.member.service.request.MemberUniversityUpdateRequest;
import cluverse.member.service.response.MemberProfileResponse;
import cluverse.university.service.UniversityService;
import cluverse.university.service.response.UniversityDetailResponse;
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
class MemberUniversityServiceTest {

    @Mock
    private MemberService memberService;

    @Mock
    private UniversityService universityService;

    @InjectMocks
    private MemberUniversityService memberUniversityService;

    @Test
    void 학교_수정시_학교_존재를_검증하고_회원_서비스에_위임한다() {
        MemberUniversityUpdateRequest request = new MemberUniversityUpdateRequest(10L);
        MemberProfileResponse response = new MemberProfileResponse(
                1L,
                "luna",
                null,
                VerificationStatus.NONE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                List.of(),
                false,
                false,
                0L,
                0L,
                0L
        );

        when(universityService.getUniversity(10L)).thenReturn(new UniversityDetailResponse(
                10L,
                "클루대",
                "clu.ac.kr",
                "https://cdn.example.com/badge.png",
                "서울",
                true
        ));
        when(memberService.updateUniversity(1L, 10L)).thenReturn(response);

        MemberProfileResponse result = memberUniversityService.updateUniversity(1L, request);

        assertThat(result).isEqualTo(response);
        verify(universityService).getUniversity(10L);
        verify(memberService).updateUniversity(1L, 10L);
    }
}
