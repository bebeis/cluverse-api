package cluverse.university.repository;

import cluverse.university.domain.University;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UniversityRepositoryTest {

    @Autowired
    private UniversityRepository universityRepository;

    @Test
    void 활성_학교를_이름순으로_조회한다() {
        // given
        universityRepository.save(University.create("클루대학교", null, null, null, true));
        universityRepository.save(University.create("가나다대학교", null, null, null, true));
        universityRepository.save(University.create("비활성대학교", null, null, null, false));

        // when
        List<University> result = universityRepository.findAllByIsActiveTrueOrderByNameAsc();

        // then
        assertThat(result).extracting(University::getName)
                .containsExactly("가나다대학교", "클루대학교");
    }

    @Test
    void 활성_학교를_키워드로_검색한다() {
        // given
        universityRepository.save(University.create("클루대학교", null, null, null, true));
        universityRepository.save(University.create("클루공과대학교", null, null, null, true));
        universityRepository.save(University.create("비활성클루대학교", null, null, null, false));

        // when
        List<University> result = universityRepository.findActiveUniversitiesByNameContaining("클루");

        // then
        assertThat(result).extracting(University::getName)
                .containsExactly("클루공과대학교", "클루대학교");
    }
}
