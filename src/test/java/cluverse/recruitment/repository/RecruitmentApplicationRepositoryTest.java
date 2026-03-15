package cluverse.recruitment.repository;

import cluverse.recruitment.domain.RecruitmentApplication;
import cluverse.recruitment.domain.FormItemAnswer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RecruitmentApplicationRepositoryTest {

    @Autowired
    private RecruitmentApplicationRepository recruitmentApplicationRepository;

    @Test
    void 지원서를_상세_조회할_수_있다() {
        // given
        RecruitmentApplication application = RecruitmentApplication.create(
                10L,
                200L,
                "Backend",
                "https://portfolio.example.com",
                List.of(FormItemAnswer.create(1L, "지원 동기입니다.")),
                "127.0.0.1"
        );
        application.addMessage(200L, "안녕하세요.", "127.0.0.1");
        RecruitmentApplication saved = recruitmentApplicationRepository.save(application);

        // when
        RecruitmentApplication result = recruitmentApplicationRepository.findById(saved.getId()).orElseThrow();

        // then
        assertThat(result.getAnswers()).hasSize(1);
        assertThat(result.getMessages()).hasSize(1);
    }

    @Test
    void 모집글과_지원자기준으로_지원서를_조회한다() {
        // given
        RecruitmentApplication saved = recruitmentApplicationRepository.save(RecruitmentApplication.create(
                10L,
                200L,
                "Backend",
                null,
                List.of(),
                "127.0.0.1"
        ));

        // when
        RecruitmentApplication result = recruitmentApplicationRepository.findByRecruitmentIdAndApplicantId(10L, 200L)
                .orElseThrow();

        // then
        assertThat(result.getId()).isEqualTo(saved.getId());
    }
}
