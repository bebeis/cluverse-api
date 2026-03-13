package cluverse.university.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.university.domain.University;
import cluverse.university.repository.UniversityRepository;
import cluverse.university.service.request.UniversitySearchRequest;
import cluverse.university.service.response.UniversitySummaryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniversityReaderTest {

    @Mock
    private UniversityRepository universityRepository;

    @InjectMocks
    private UniversityReader universityReader;

    @Test
    void 키워드가_없으면_활성_학교_전체를_조회한다() {
        // given
        when(universityRepository.findAllByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(
                createUniversity(1L, "가나다대학교", null),
                createUniversity(2L, "클루대학교", "badge")
        ));

        // when
        List<UniversitySummaryResponse> result = universityReader.search(new UniversitySearchRequest(" "));

        // then
        assertThat(result).containsExactly(
                new UniversitySummaryResponse(1L, "가나다대학교", null),
                new UniversitySummaryResponse(2L, "클루대학교", "badge")
        );
        verify(universityRepository).findAllByIsActiveTrueOrderByNameAsc();
    }

    @Test
    void 키워드가_있으면_학교명을_검색한다() {
        // given
        when(universityRepository.findActiveUniversitiesByNameContaining("클루")).thenReturn(List.of(
                createUniversity(2L, "클루대학교", "badge")
        ));

        // when
        List<UniversitySummaryResponse> result = universityReader.search(new UniversitySearchRequest("클루"));

        // then
        assertThat(result).containsExactly(
                new UniversitySummaryResponse(2L, "클루대학교", "badge")
        );
    }

    @Test
    void 학교를_조회할_수_있다() {
        // given
        University university = createUniversity(1L, "클루대학교", "badge");
        when(universityRepository.findById(1L)).thenReturn(Optional.of(university));

        // when
        University result = universityReader.readOrThrow(1L);

        // then
        assertThat(result).isSameAs(university);
    }

    @Test
    void 학교가_없으면_예외가_발생한다() {
        // given
        when(universityRepository.findById(999L)).thenReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> universityReader.readOrThrow(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 학교입니다.");
    }

    private University createUniversity(Long id, String name, String badgeImageUrl) {
        University university = University.create(name, null, badgeImageUrl, null, true);
        ReflectionTestUtils.setField(university, "id", id);
        return university;
    }
}
