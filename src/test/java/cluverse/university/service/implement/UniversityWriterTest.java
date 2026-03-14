package cluverse.university.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.university.domain.University;
import cluverse.university.repository.UniversityRepository;
import cluverse.university.service.request.UniversityCreateRequest;
import cluverse.university.service.request.UniversityUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniversityWriterTest {

    @Mock
    private UniversityRepository universityRepository;

    @InjectMocks
    private UniversityWriter universityWriter;

    @Test
    void 관리자는_학교를_등록할_수_있다() {
        // given
        UniversityCreateRequest request = new UniversityCreateRequest(
                " 클루대학교 ",
                " cluverse.ac.kr ",
                " https://cdn.example.com/badge.png ",
                " 서울 ",
                true
        );
        when(universityRepository.existsByName("클루대학교")).thenReturn(false);
        when(universityRepository.save(any(University.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        University result = universityWriter.create(request);

        // then
        ArgumentCaptor<University> universityCaptor = ArgumentCaptor.forClass(University.class);
        verify(universityRepository).save(universityCaptor.capture());
        assertThat(result.getName()).isEqualTo("클루대학교");
        assertThat(result.getEmailDomain()).isEqualTo("cluverse.ac.kr");
        assertThat(result.getBadgeImageUrl()).isEqualTo("https://cdn.example.com/badge.png");
        assertThat(result.getAddress()).isEqualTo("서울");
    }

    @Test
    void 중복된_학교명으로_등록할_수_없다() {
        // given
        when(universityRepository.existsByName("클루대학교")).thenReturn(true);

        // when, then
        assertThatThrownBy(() -> universityWriter.create(new UniversityCreateRequest(
                "클루대학교",
                null,
                null,
                null,
                true
        )))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("이미 등록된 학교명입니다.");
    }

    @Test
    void 관리자는_학교를_수정할_수_있다() {
        // given
        University university = University.create("클루대학교", null, null, null, true);
        ReflectionTestUtils.setField(university, "id", 10L);
        when(universityRepository.existsByNameAndIdNot("클루대학교", 10L)).thenReturn(false);

        // when
        universityWriter.update(university, new UniversityUpdateRequest(
                " 클루대학교 ",
                " cluverse.ac.kr ",
                " https://cdn.example.com/badge-v2.png ",
                " 부산 ",
                false
        ));

        // then
        assertThat(university.getName()).isEqualTo("클루대학교");
        assertThat(university.getEmailDomain()).isEqualTo("cluverse.ac.kr");
        assertThat(university.getBadgeImageUrl()).isEqualTo("https://cdn.example.com/badge-v2.png");
        assertThat(university.getAddress()).isEqualTo("부산");
        assertThat(university.isActive()).isFalse();
    }
}
