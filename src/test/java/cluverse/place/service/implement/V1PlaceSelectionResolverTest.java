package cluverse.place.service.implement;

import cluverse.place.client.PlaceSearchClient;
import cluverse.place.domain.PlaceCandidate;
import cluverse.place.domain.PlaceProvider;
import cluverse.place.domain.PlaceSourceCandidate;
import cluverse.place.service.request.PlaceSelectionRequestV1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class V1PlaceSelectionResolverTest {

    @Mock
    private PlaceSearchClient placeSearchClient;
    @InjectMocks
    private V1PlaceSelectionResolver resolver;

    @Test
    void 같은_검색어의_여러_장소는_외부_API를_한_번만_호출해_검증한다() {
        PlaceSourceCandidate firstSource = source("첫 장소");
        PlaceSourceCandidate secondSource = source("둘째 장소");
        PlaceCandidate first = PlaceCandidateFactory.create(firstSource);
        PlaceCandidate second = PlaceCandidateFactory.create(secondSource);
        given(placeSearchClient.search("연세대 맛집")).willReturn(List.of(firstSource, secondSource));

        var result = resolver.resolve(List.of(
                new PlaceSelectionRequestV1("연세대 맛집", first.sourceFingerprint(), true),
                new PlaceSelectionRequestV1("연세대 맛집", second.sourceFingerprint(), false)
        ));

        assertThat(result).extracting(value -> value.candidate().name())
                .containsExactly("첫 장소", "둘째 장소");
        verify(placeSearchClient, times(1)).search("연세대 맛집");
    }

    private PlaceSourceCandidate source(String name) {
        return new PlaceSourceCandidate(
                PlaceProvider.NAVER, name, "음식점", "주소", "도로명",
                new BigDecimal("37.5500000"), new BigDecimal("126.9400000"), null
        );
    }
}
