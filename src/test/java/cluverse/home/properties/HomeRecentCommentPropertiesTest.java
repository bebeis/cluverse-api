package cluverse.home.properties;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HomeRecentCommentPropertiesTest {

    @Test
    void 음수_캐시_TTL은_허용하지_않는다() {
        assertThatThrownBy(() -> new HomeRecentCommentProperties(Duration.ofSeconds(-1), 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("snapshotCacheTtl은 0 이상이어야 합니다.");
    }
}
