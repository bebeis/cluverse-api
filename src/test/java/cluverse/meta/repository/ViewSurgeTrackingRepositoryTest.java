package cluverse.meta.repository;

import cluverse.meta.domain.ViewSurgeTracking;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// UPSERT(ON DUPLICATE KEY)는 MySQL 문법 — 임베디드 교체 대신 MODE=MySQL H2 설정을 그대로 쓴다
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ViewSurgeTrackingRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 26, 12, 0, 0);

    @Autowired
    private ViewSurgeTrackingRepository viewSurgeTrackingRepository;

    @Test
    void 급상승_등록_UPSERT는_행이_없으면_생성한다() {
        // when
        int affectedRowCount = viewSurgeTrackingRepository.upsertActivation(10L, NOW, NOW.plusMinutes(5));

        // then
        assertThat(affectedRowCount).isEqualTo(1);
        assertThat(viewSurgeTrackingRepository.findById(10L))
                .get()
                .extracting(ViewSurgeTracking::getExpiresAt)
                .isEqualTo(NOW.plusMinutes(5));
    }

    @Test
    void 이미_추적_중이면_만료_시각을_뒤로만_민다() {
        // given
        viewSurgeTrackingRepository.upsertActivation(10L, NOW, NOW.plusMinutes(5));

        // when — 더 이른 만료로 재등록해도 기존 값이 유지된다 (GREATEST)
        viewSurgeTrackingRepository.upsertActivation(10L, NOW, NOW.plusMinutes(3));

        // then
        assertThat(viewSurgeTrackingRepository.findById(10L))
                .get()
                .extracting(ViewSurgeTracking::getExpiresAt)
                .isEqualTo(NOW.plusMinutes(5));
    }

    @Test
    void 더_늦은_만료로_재등록하면_만료_시각이_연장된다() {
        // given
        viewSurgeTrackingRepository.upsertActivation(10L, NOW, NOW.plusMinutes(5));

        // when
        viewSurgeTrackingRepository.upsertActivation(10L, NOW, NOW.plusMinutes(10));

        // then
        assertThat(viewSurgeTrackingRepository.findById(10L))
                .get()
                .extracting(ViewSurgeTracking::getExpiresAt)
                .isEqualTo(NOW.plusMinutes(10));
    }

    @Test
    void 만료_시각을_여러_게시글에_한_번에_연장할_수_있다() {
        // given
        viewSurgeTrackingRepository.upsertActivation(10L, NOW, NOW.plusMinutes(5));
        viewSurgeTrackingRepository.upsertActivation(11L, NOW, NOW.plusMinutes(5));
        viewSurgeTrackingRepository.upsertActivation(12L, NOW, NOW.plusMinutes(5));

        // when
        int updatedRowCount = viewSurgeTrackingRepository.extendExpiryAll(List.of(10L, 11L), NOW.plusMinutes(15));

        // then
        assertThat(updatedRowCount).isEqualTo(2);
        assertThat(viewSurgeTrackingRepository.findById(10L))
                .get()
                .extracting(ViewSurgeTracking::getExpiresAt)
                .isEqualTo(NOW.plusMinutes(15));
        assertThat(viewSurgeTrackingRepository.findById(12L))
                .get()
                .extracting(ViewSurgeTracking::getExpiresAt)
                .isEqualTo(NOW.plusMinutes(5));
    }

    @Test
    void 더_이른_시각으로_연장을_시도해도_만료가_되감기지_않는다() {
        // given — 다중 인스턴스 flush 경합: 늦게 커밋된 짧은 만료가 되감으면 안 된다
        viewSurgeTrackingRepository.upsertActivation(10L, NOW, NOW.plusMinutes(10));

        // when
        int updatedRowCount = viewSurgeTrackingRepository.extendExpiryAll(List.of(10L), NOW.plusMinutes(3));

        // then
        assertThat(updatedRowCount).isEqualTo(1);
        assertThat(viewSurgeTrackingRepository.findById(10L))
                .get()
                .extracting(ViewSurgeTracking::getExpiresAt)
                .isEqualTo(NOW.plusMinutes(10));
    }

    @Test
    void 활성_게시글은_만료가_늦은_순으로_상한까지만_조회한다() {
        // given
        viewSurgeTrackingRepository.upsertActivation(1L, NOW, NOW.plusMinutes(1));
        viewSurgeTrackingRepository.upsertActivation(2L, NOW, NOW.plusMinutes(10));
        viewSurgeTrackingRepository.upsertActivation(3L, NOW, NOW.plusMinutes(5));
        viewSurgeTrackingRepository.upsertActivation(4L, NOW, NOW.minusMinutes(1));

        // when
        List<Long> activePostIds = viewSurgeTrackingRepository.findActivePostIds(NOW, 2);

        // then — 만료된 4번은 제외, 늦은 만료 순 상위 2개
        assertThat(activePostIds).containsExactly(2L, 3L);
    }

    @Test
    void 정리_대상은_유예_컷오프가_지난_행만_조회한다() {
        // given
        viewSurgeTrackingRepository.upsertActivation(1L, NOW.minusMinutes(10), NOW.minusMinutes(1));
        viewSurgeTrackingRepository.upsertActivation(2L, NOW.minusMinutes(10), NOW.minusSeconds(5));
        viewSurgeTrackingRepository.upsertActivation(3L, NOW, NOW.plusMinutes(5));

        // when — 컷오프 = NOW - 15초
        List<Long> expiredPostIds = viewSurgeTrackingRepository.findExpiredPostIds(NOW.minusSeconds(15), 100);

        // then — 2번은 만료됐지만 유예가 안 지났고, 3번은 활성
        assertThat(expiredPostIds).containsExactly(1L);
    }
}
