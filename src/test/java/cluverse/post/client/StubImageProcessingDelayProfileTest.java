package cluverse.post.client;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class StubImageProcessingDelayProfileTest {

    @Test
    void 평균_920ms를_중심으로_재현_가능한_꼬리_지연을_만든다() {
        StubImageProcessingDelayProfile profile = new StubImageProcessingDelayProfile(
                Duration.ofMillis(920));

        assertThat(profile.delayForBucket(0)).isEqualTo(Duration.ofMillis(900));
        assertThat(profile.delayForBucket(74)).isEqualTo(Duration.ofMillis(900));
        assertThat(profile.delayForBucket(75)).isEqualTo(Duration.ofMillis(960));
        assertThat(profile.delayForBucket(94)).isEqualTo(Duration.ofMillis(960));
        assertThat(profile.delayForBucket(95)).isEqualTo(Duration.ofMillis(1_040));
        assertThat(profile.delayForBucket(99)).isEqualTo(Duration.ofMillis(1_040));

        long weightedAverageMillis = (
                75 * profile.delayForBucket(0).toMillis()
                        + 20 * profile.delayForBucket(75).toMillis()
                        + 5 * profile.delayForBucket(95).toMillis()
        ) / 100;
        assertThat(weightedAverageMillis).isEqualTo(919);
    }
}
