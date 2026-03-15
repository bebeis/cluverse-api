package cluverse.recruitment.repository;

import cluverse.recruitment.domain.FormItem;
import cluverse.recruitment.domain.FormItemQuestionType;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.domain.RecruitmentPosition;
import cluverse.recruitment.domain.RecruitmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RecruitmentRepositoryTest {

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    @Test
    void 모집글_상세_조회시_질문항목을_함께_가져온다() {
        // given
        Recruitment recruitment = Recruitment.create(
                1L,
                100L,
                "백엔드 모집",
                "설명",
                List.of(new RecruitmentPosition("Backend", 2)),
                "Spring 경험",
                "3개월",
                "MVP",
                "주 2회 회의",
                LocalDateTime.of(2026, 3, 31, 23, 59),
                List.of(FormItem.create("지원 동기를 적어주세요.", FormItemQuestionType.TEXT, true, List.of(), 1))
        );
        Recruitment saved = recruitmentRepository.save(recruitment);

        // when
        Recruitment result = recruitmentRepository.findById(saved.getId()).orElseThrow();

        // then
        assertThat(result.getFormItems()).hasSize(1);
        assertThat(result.getPositions()).hasSize(1);
    }

    @Test
    void 삭제되지_않은_오픈_모집글_개수를_센다() {
        // given
        Recruitment activeRecruitment = recruitmentRepository.save(Recruitment.create(
                1L,
                100L,
                "백엔드 모집",
                "설명",
                List.of(),
                null,
                null,
                null,
                null,
                null,
                List.of()
        ));
        Recruitment deletedRecruitment = recruitmentRepository.save(Recruitment.create(
                1L,
                100L,
                "디자인 모집",
                "설명",
                List.of(),
                null,
                null,
                null,
                null,
                null,
                List.of()
        ));
        deletedRecruitment.delete();
        recruitmentRepository.flush();

        // when
        long result = recruitmentRepository.countByGroupIdAndStatusAndDeletedAtIsNull(1L, RecruitmentStatus.OPEN);

        // then
        assertThat(result).isEqualTo(1L);
        assertThat(activeRecruitment.isDeleted()).isFalse();
    }
}
