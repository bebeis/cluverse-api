package cluverse.place.client;

import cluverse.place.domain.PlaceCategory;
import cluverse.place.domain.PlaceSourceCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NaverPlaceResponseMapperTest {

    private final NaverPlaceResponseMapper mapper = new NaverPlaceResponseMapper();

    @Test
    void 네이버_HTML과_WGS84_정수_좌표를_내부_후보로_변환한다() {
        NaverLocalSearchResponse response = new NaverLocalSearchResponse(List.of(
                new NaverLocalSearchResponse.Item(
                        "<b>클루버스</b> &amp; 카페",
                        "https://map.naver.com/example",
                        "카페,디저트>카페",
                        "서울특별시 광진구 화양동 1",
                        "서울특별시 광진구 능동로 1",
                        1269873882L,
                        375666103L
                )
        ));

        PlaceSourceCandidate candidate = mapper.map(response).getFirst();

        assertThat(candidate.name()).isEqualTo("클루버스 & 카페");
        assertThat(candidate.longitude()).isEqualByComparingTo("126.9873882");
        assertThat(candidate.latitude()).isEqualByComparingTo("37.5666103");
        assertThat(PlaceCategory.from(candidate.rawCategory())).isEqualTo(PlaceCategory.CAFE);
    }

    @Test
    void 비어있는_링크는_null로_정규화한다() {
        NaverLocalSearchResponse response = new NaverLocalSearchResponse(List.of(
                new NaverLocalSearchResponse.Item(
                        "장소",
                        " ",
                        "한식",
                        "주소",
                        "도로명 주소",
                        1270000000L,
                        370000000L
                )
        ));

        assertThat(mapper.map(response).getFirst().sourceUrl()).isNull();
    }
}
