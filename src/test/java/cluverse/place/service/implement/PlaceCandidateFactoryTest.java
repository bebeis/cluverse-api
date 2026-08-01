package cluverse.place.service.implement;

import cluverse.place.domain.PlaceProvider;
import cluverse.place.domain.PlaceSourceCandidate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceCandidateFactoryTest {

    @Test
    void 공백과_대소문자가_달라도_같은_장소_fingerprint를_만든다() {
        PlaceSourceCandidate first = candidate("  Cluverse   Cafe  ", "서울 광진구  1", "37.1234000");
        PlaceSourceCandidate second = candidate("cluverse cafe", "서울 광진구 1", "37.1234");

        assertThat(PlaceCandidateFactory.create(first).sourceFingerprint())
                .isEqualTo(PlaceCandidateFactory.create(second).sourceFingerprint());
    }

    @Test
    void 주소나_좌표가_다르면_다른_fingerprint를_만든다() {
        PlaceSourceCandidate first = candidate("클루버스 카페", "서울 광진구 1", "37.1234000");
        PlaceSourceCandidate second = candidate("클루버스 카페", "서울 광진구 2", "37.1235000");

        assertThat(PlaceCandidateFactory.create(first).sourceFingerprint())
                .isNotEqualTo(PlaceCandidateFactory.create(second).sourceFingerprint());
    }

    private PlaceSourceCandidate candidate(String name, String roadAddress, String latitude) {
        return new PlaceSourceCandidate(
                PlaceProvider.NAVER,
                name,
                "카페,디저트",
                "서울 광진구 화양동 1",
                roadAddress,
                new BigDecimal(latitude),
                new BigDecimal("127.1234000"),
                "https://map.naver.com/example"
        );
    }
}
