package cluverse.popularity.service.implement;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PopularityScoreTest {

    @Test
    void 좋아요와_댓글만으로_인기_점수를_계산한다() {
        PopularityScore popularityScore = new PopularityScore(3, 2);

        assertThat(popularityScore.calculate(10, 4)).isEqualTo(38L);
    }
}
