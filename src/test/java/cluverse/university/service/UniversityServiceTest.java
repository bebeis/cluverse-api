package cluverse.university.service;

import cluverse.common.exception.ForbiddenException;
import cluverse.member.service.implement.MemberReader;
import cluverse.university.domain.University;
import cluverse.university.service.implement.UniversityReader;
import cluverse.university.service.implement.UniversityWriter;
import cluverse.university.service.request.UniversityCreateRequest;
import cluverse.university.service.request.UniversitySearchRequest;
import cluverse.university.service.request.UniversityUpdateRequest;
import cluverse.university.service.response.UniversityDetailResponse;
import cluverse.university.service.response.UniversitySummaryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniversityServiceTest {

    @Mock
    private UniversityReader universityReader;

    @Mock
    private UniversityWriter universityWriter;

    @Mock
    private MemberReader memberReader;

    @InjectMocks
    private UniversityQueryService universityQueryService;

    @InjectMocks
    private UniversityService universityService;

    @Test
    void 학교_검색은_reader에_위임한다() {
        // given
        UniversitySearchRequest request = new UniversitySearchRequest("클루");
        List<UniversitySummaryResponse> responses = List.of(
                new UniversitySummaryResponse(1L, "클루대학교", "badge")
        );
        when(universityReader.search(request)).thenReturn(responses);

        // when
        List<UniversitySummaryResponse> result = universityQueryService.searchUniversities(request);

        // then
        assertThat(result).isEqualTo(responses);
        verify(universityReader).search(request);
    }

    @Test
    void 학교_상세_조회시_응답으로_변환한다() {
        // given
        University university = createUniversity(1L, "클루대학교", "cluverse.ac.kr", "badge", "서울", true);
        when(universityReader.readOrThrow(1L)).thenReturn(university);

        // when
        UniversityDetailResponse result = universityQueryService.getUniversity(1L);

        // then
        assertThat(result.universityId()).isEqualTo(1L);
        assertThat(result.universityName()).isEqualTo("클루대학교");
        assertThat(result.emailDomain()).isEqualTo("cluverse.ac.kr");
        verify(universityReader).readOrThrow(1L);
    }

    @Test
    void 학교_등록시_관리자_회원으로_등록한다() {
        // given
        UniversityCreateRequest request = new UniversityCreateRequest(
                "클루대학교",
                "cluverse.ac.kr",
                "badge",
                "서울",
                true
        );
        University university = createUniversity(1L, "클루대학교", "cluverse.ac.kr", "badge", "서울", true);
        when(memberReader.isAdmin(10L)).thenReturn(true);
        when(universityWriter.create(request)).thenReturn(university);

        // when
        UniversityDetailResponse result = universityService.createUniversity(10L, request);

        // then
        assertThat(result.universityId()).isEqualTo(1L);
        assertThat(result.universityName()).isEqualTo("클루대학교");
        verify(memberReader).isAdmin(10L);
        verify(universityWriter).create(request);
    }

    @Test
    void 학교_수정시_학교와_관리자를_조회한다() {
        // given
        University university = createUniversity(1L, "클루대학교", "cluverse.ac.kr", "badge", "서울", true);
        UniversityUpdateRequest request = new UniversityUpdateRequest(
                "클루대학교",
                "cluverse.ac.kr",
                "badge-v2",
                "부산",
                false
        );
        when(memberReader.isAdmin(10L)).thenReturn(true);
        when(universityReader.readOrThrow(1L)).thenReturn(university);

        // when
        UniversityDetailResponse result = universityService.updateUniversity(10L, 1L, request);

        // then
        assertThat(result.universityId()).isEqualTo(1L);
        verify(memberReader).isAdmin(10L);
        verify(universityReader).readOrThrow(1L);
        verify(universityWriter).update(university, request);
    }

    @Test
    void 관리자가_아니면_학교를_등록할_수_없다() {
        // given
        UniversityCreateRequest request = new UniversityCreateRequest(
                "클루대학교",
                "cluverse.ac.kr",
                "badge",
                "서울",
                true
        );
        when(memberReader.isAdmin(10L)).thenReturn(false);

        // when, then
        assertThatThrownBy(() -> universityService.createUniversity(10L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("학교 관리 권한이 없습니다.");
    }

    private University createUniversity(
            Long universityId,
            String name,
            String emailDomain,
            String badgeImageUrl,
            String address,
            boolean isActive
    ) {
        University university = University.create(name, emailDomain, badgeImageUrl, address, isActive);
        ReflectionTestUtils.setField(university, "id", universityId);
        return university;
    }
}
