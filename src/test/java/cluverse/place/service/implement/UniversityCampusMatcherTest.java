package cluverse.place.service.implement;

import cluverse.place.domain.PlaceCandidate;
import cluverse.place.domain.PlaceCategory;
import cluverse.place.domain.PlaceProvider;
import cluverse.university.domain.UniversityCampus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UniversityCampusMatcherTest {

    private final UniversityCampusMatcher matcher = new UniversityCampusMatcher();

    @Test
    void 반경_안에_있는_가장_가까운_캠퍼스를_선택한다() {
        PlaceCandidate place = candidate("37.5510000", "126.9410000");
        UniversityCampus near = UniversityCampus.create(
                1L, "신촌", new BigDecimal("37.5511000"), new BigDecimal("126.9411000"), 1_000);
        UniversityCampus far = UniversityCampus.create(
                1L, "멀리", new BigDecimal("37.5600000"), new BigDecimal("126.9500000"), 2_000);

        assertThat(matcher.findNearestInRadius(place, List.of(far, near))).contains(near);
    }

    @Test
    void 모든_캠퍼스의_반경_밖이면_선택하지_않는다() {
        PlaceCandidate place = candidate("37.5510000", "126.9410000");
        UniversityCampus campus = UniversityCampus.create(
                1L, "신촌", new BigDecimal("37.5600000"), new BigDecimal("126.9500000"), 100);

        assertThat(matcher.findNearestInRadius(place, List.of(campus))).isEmpty();
    }

    private PlaceCandidate candidate(String latitude, String longitude) {
        return new PlaceCandidate(
                PlaceProvider.NAVER, "fingerprint", "장소", PlaceCategory.FOOD, "음식점",
                "주소", "도로명", new BigDecimal(latitude), new BigDecimal(longitude), null
        );
    }
}
