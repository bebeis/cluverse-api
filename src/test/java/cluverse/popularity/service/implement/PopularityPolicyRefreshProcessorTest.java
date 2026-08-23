package cluverse.popularity.service.implement;

import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.PopularityQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularityPolicyRefreshProcessorTest {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Instant NOW = Instant.parse("2026-08-01T03:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.ofInstant(NOW, ZONE_ID);

    @Mock
    private PopularityQueryRepository popularityQueryRepository;

    @Mock
    private PopularityPolicyBoardRefreshProcessor popularityPolicyBoardRefreshProcessor;

    @Test
    void 한_게시판의_갱신이_실패해도_다음_게시판을_계속_갱신한다() {
        // given
        PopularityProperties properties = properties();
        LocalDateTime sampleEnd = NOW_LOCAL.minus(properties.promotionWindow());
        LocalDateTime sampleStart = sampleEnd.minus(properties.policySampleWindow());
        when(popularityQueryRepository.findPolicyBoardIds(sampleStart, sampleEnd))
                .thenReturn(List.of(10L, 20L));
        doThrow(new IllegalStateException("갱신 실패"))
                .when(popularityPolicyBoardRefreshProcessor)
                .refreshBoard(10L, sampleStart, sampleEnd, NOW_LOCAL);
        PopularityPolicyRefreshProcessor processor = new PopularityPolicyRefreshProcessor(
                popularityQueryRepository,
                popularityPolicyBoardRefreshProcessor,
                properties,
                Clock.fixed(NOW, ZONE_ID)
        );

        // when
        int refreshed = processor.refresh();

        // then
        assertThat(refreshed).isEqualTo(1);
        verify(popularityPolicyBoardRefreshProcessor)
                .refreshBoard(20L, sampleStart, sampleEnd, NOW_LOCAL);
    }

    private PopularityProperties properties() {
        return new PopularityProperties(
                100L, 3, 2,
                Duration.ofHours(48), Duration.ofDays(7), 0.98, 100, 0.30,
                Duration.ofSeconds(30), 500
        );
    }
}
