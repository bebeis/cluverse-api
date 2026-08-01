package cluverse.meta.service.implement;

import cluverse.meta.properties.ViewSurgeProperties;
import cluverse.popularity.service.implement.PopularityPromotionInvoker;
import cluverse.popularity.domain.PopularityTrigger;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ViewSurgeDetectorTest {

    private static final Duration WINDOW = Duration.ofSeconds(10);
    private static final long THRESHOLD = 5L;

    @Mock
    private ViewSurgeTrackingWriter viewSurgeTrackingWriter;

    @Mock
    private PopularityPromotionInvoker popularityPromotionInvoker;

    private MutableClock clock;
    private ViewSurgeDetector viewSurgeDetector;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-07-26T12:00:00Z"));
        viewSurgeDetector = new ViewSurgeDetector(
                createProperties(),
                viewSurgeTrackingWriter,
                popularityPromotionInvoker,
                clock,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void 첫_관측은_기준점만_기록하고_판정하지_않는다() {
        // when
        viewSurgeDetector.observe(10L, 100L);

        // then
        verify(viewSurgeTrackingWriter, never()).activate(any(), any());
    }

    @Test
    void 관측_구간이_끝나기_전에는_판정하지_않는다() {
        // given
        viewSurgeDetector.observe(10L, 100L);
        clock.advance(Duration.ofSeconds(5));

        // when
        viewSurgeDetector.observe(10L, 1_000L);

        // then
        verify(viewSurgeTrackingWriter, never()).activate(any(), any());
    }

    @Test
    void 관측_구간_동안_증가량이_기준을_넘으면_급상승으로_등록한다() {
        // given
        viewSurgeDetector.observe(10L, 100L);
        clock.advance(Duration.ofSeconds(10));

        // when
        viewSurgeDetector.observe(10L, 105L);

        // then
        verify(viewSurgeTrackingWriter).activate(eq(10L), any(Instant.class));
        verify(popularityPromotionInvoker).tryEvaluate(10L, PopularityTrigger.SURGE_ACTIVATED);
    }

    @Test
    void 관측_구간_동안_증가량이_기준_미만이면_등록하지_않는다() {
        // given
        viewSurgeDetector.observe(10L, 100L);
        clock.advance(Duration.ofSeconds(10));

        // when
        viewSurgeDetector.observe(10L, 104L);

        // then
        verify(viewSurgeTrackingWriter, never()).activate(any(), any());
    }

    @Test
    void 관측_구간을_크게_초과하면_증가량이_커도_판정하지_않고_기준점만_갱신한다() {
        // given — 재려는 것은 증가 "량"이 아니라 증가 "속도"다
        viewSurgeDetector.observe(10L, 100L);
        clock.advance(Duration.ofSeconds(31));

        // when
        viewSurgeDetector.observe(10L, 100_000L);

        // then
        verify(viewSurgeTrackingWriter, never()).activate(any(), any());
    }

    @Test
    void 판정_후에는_기준점이_갱신되어_다음_구간을_새로_관측한다() {
        // given
        viewSurgeDetector.observe(10L, 100L);
        clock.advance(Duration.ofSeconds(10));
        viewSurgeDetector.observe(10L, 110L);

        // when — 새 구간에서는 증가량이 기준 미만
        clock.advance(Duration.ofSeconds(10));
        viewSurgeDetector.observe(10L, 112L);

        // then
        verify(viewSurgeTrackingWriter, times(1)).activate(eq(10L), any(Instant.class));
    }

    private ViewSurgeProperties createProperties() {
        return new ViewSurgeProperties(
                WINDOW,
                THRESHOLD,
                3L,
                Duration.ofMinutes(5),
                Duration.ofMinutes(5),
                Duration.ofSeconds(15),
                1_000,
                Duration.ofSeconds(60),
                1_000,
                100
        );
    }

    private static class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("Asia/Seoul");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
