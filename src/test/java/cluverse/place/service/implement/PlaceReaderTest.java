package cluverse.place.service.implement;

import cluverse.place.domain.PlaceCategory;
import cluverse.place.repository.PlaceQueryRepository;
import cluverse.place.repository.PlaceRepository;
import cluverse.place.repository.dto.LocalMapMarkerQueryResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PlaceReaderTest {

    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private PlaceQueryRepository placeQueryRepository;
    @InjectMocks
    private PlaceReader placeReader;

    @Test
    void 로컬맵_마커_조회는_장소_조회_도구가_담당한다() {
        LocalMapMarkerQueryResult marker = new LocalMapMarkerQueryResult(
                1L, "클루버스 카페", "CAFE", "주소", "도로명",
                new BigDecimal("37.1234567"), new BigDecimal("127.1234567"), 3L,
                LocalDateTime.of(2026, 8, 1, 12, 0));
        given(placeQueryRepository.findMarkers(1L, 2L, PlaceCategory.CAFE)).willReturn(List.of(marker));

        var result = placeReader.readMarkers(1L, 2L, PlaceCategory.CAFE);

        assertThat(result).containsExactly(marker);
    }
}
