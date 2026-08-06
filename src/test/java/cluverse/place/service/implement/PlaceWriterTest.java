package cluverse.place.service.implement;

import cluverse.place.domain.Place;
import cluverse.place.domain.PlaceCandidate;
import cluverse.place.domain.PlaceCategory;
import cluverse.place.domain.PlaceProvider;
import cluverse.place.repository.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaceWriterTest {

    @Mock
    private PlaceRepository placeRepository;
    @InjectMocks
    private PlaceWriter placeWriter;

    @Test
    void upsert가_확정한_connection_local_ID로_Place_reference를_반환한다() {
        PlaceCandidate candidate = candidate();
        Place place = org.mockito.Mockito.mock(Place.class);
        given(placeRepository.lastInsertedId()).willReturn(42L);
        given(placeRepository.getReferenceById(42L)).willReturn(place);

        Place result = placeWriter.upsert(candidate);

        assertThat(result).isSameAs(place);
        verify(placeRepository).lastInsertedId();
        verify(placeRepository).getReferenceById(42L);
    }

    private PlaceCandidate candidate() {
        return new PlaceCandidate(
                PlaceProvider.NAVER,
                "fingerprint",
                "클루버스 카페",
                PlaceCategory.CAFE,
                "음식점>카페,디저트",
                "주소",
                "도로명 주소",
                new BigDecimal("37.5510000"),
                new BigDecimal("126.9410000"),
                "https://example.test/places/cafe"
        );
    }
}
