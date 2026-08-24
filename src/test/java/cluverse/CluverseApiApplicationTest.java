package cluverse;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class CluverseApiApplicationTest {

    @Test
    void 애플리케이션_기본_시간대는_서울이다() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        CluverseApiApplication.configureDefaultTimeZone();

        assertThat(TimeZone.getDefault().toZoneId())
                .isEqualTo(ZoneId.of("Asia/Seoul"));
    }
}
