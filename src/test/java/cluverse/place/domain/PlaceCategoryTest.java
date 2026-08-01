package cluverse.place.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceCategoryTest {

    @Test
    void 네이버_카페와_음식점_카테고리를_내부_카테고리로_변환한다() {
        assertThat(PlaceCategory.from("카페,디저트>카페")).isEqualTo(PlaceCategory.CAFE);
        assertThat(PlaceCategory.from("한식>육류,고기요리")).isEqualTo(PlaceCategory.FOOD);
    }

    @Test
    void 로컬맵_대상이_아닌_카테고리는_OTHER로_변환한다() {
        assertThat(PlaceCategory.from("교육,학문>대학교")).isEqualTo(PlaceCategory.OTHER);
    }
}
