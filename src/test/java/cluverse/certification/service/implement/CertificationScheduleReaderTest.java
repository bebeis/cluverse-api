package cluverse.certification.service.implement;

import cluverse.certification.client.CertificationScheduleClient;
import cluverse.certification.domain.CertificationDeadline;
import cluverse.certification.domain.CertificationExamPhase;
import cluverse.certification.domain.CertificationSchedule;
import cluverse.certification.properties.CertificationProperties;
import cluverse.certification.properties.CertificationProviderMode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CertificationScheduleReaderTest {

    @Test
    void 지난_마감은_제외하고_올해와_다음_해_일정을_마감_임박순으로_반환한다() {
        // given
        LocalDate today = LocalDate.of(2026, 8, 3);
        CertificationScheduleClient client = mock(CertificationScheduleClient.class);
        when(client.readSchedules(2026)).thenReturn(List.of(
                schedule("이미 마감", today.minusDays(1), null),
                schedule("가까운 일정", today.plusDays(2), today.plusDays(5))
        ));
        when(client.readSchedules(2027)).thenReturn(List.of(
                schedule("다음 해 일정", LocalDate.of(2027, 1, 2), null)
        ));
        CertificationScheduleReader reader = new CertificationScheduleReader(client, properties());

        // when
        List<CertificationDeadline> first = reader.readUpcomingDeadlines(today, 10);
        List<CertificationDeadline> second = reader.readUpcomingDeadlines(today, 10);

        // then
        assertThat(first).extracting(CertificationDeadline::description)
                .containsExactly("가까운 일정", "가까운 일정", "다음 해 일정");
        assertThat(first).extracting(CertificationDeadline::phase)
                .containsExactly(
                        CertificationExamPhase.WRITTEN,
                        CertificationExamPhase.PRACTICAL,
                        CertificationExamPhase.WRITTEN
                );
        assertThat(first).extracting(CertificationDeadline::registrationEndDate)
                .containsExactly(
                        today.plusDays(2),
                        today.plusDays(5),
                        LocalDate.of(2027, 1, 2)
                );
        assertThat(second).isEqualTo(first);
        verify(client).readSchedules(2026);
        verify(client).readSchedules(2027);
    }

    @Test
    void 캐시를_비우면_연도별_일정을_다시_조회한다() {
        // given
        LocalDate today = LocalDate.of(2026, 8, 3);
        CertificationScheduleClient client = mock(CertificationScheduleClient.class);
        when(client.readSchedules(2026)).thenReturn(List.of());
        when(client.readSchedules(2027)).thenReturn(List.of());
        CertificationScheduleReader reader = new CertificationScheduleReader(client, properties());
        reader.readUpcomingDeadlines(today, 10);

        // when
        reader.evictAll();
        reader.readUpcomingDeadlines(today, 10);

        // then
        verify(client, times(2)).readSchedules(2026);
        verify(client, times(2)).readSchedules(2027);
    }

    private CertificationSchedule schedule(
            String description,
            LocalDate writtenEndDate,
            LocalDate practicalEndDate
    ) {
        return new CertificationSchedule(
                "국가기술자격",
                description,
                writtenEndDate.minusDays(2),
                writtenEndDate,
                writtenEndDate.plusDays(20),
                writtenEndDate.plusDays(21),
                practicalEndDate == null ? null : practicalEndDate.minusDays(2),
                practicalEndDate,
                practicalEndDate == null ? null : practicalEndDate.plusDays(20),
                practicalEndDate == null ? null : practicalEndDate.plusDays(21)
        );
    }

    private CertificationProperties properties() {
        return new CertificationProperties(
                CertificationProviderMode.STUB,
                "https://apis.data.go.kr",
                "test-key",
                Duration.ofMillis(500),
                Duration.ofSeconds(2),
                Duration.ofHours(12),
                false,
                ""
        );
    }
}
