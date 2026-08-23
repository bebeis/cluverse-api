package cluverse.place.service;

import cluverse.place.client.PlaceSearchClient;
import cluverse.place.domain.PlaceCandidate;
import cluverse.place.domain.PlaceProvider;
import cluverse.place.domain.PlaceSourceCandidate;
import cluverse.place.service.implement.PlaceCandidateFactory;
import cluverse.place.service.implement.PlaceSelectionTokenManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceSearchServiceTest {

    @Mock
    private PlaceSearchClient placeSearchClient;

    @Mock
    private PlaceSelectionTokenManager tokenManager;

    @InjectMocks
    private PlaceSearchService placeSearchService;

    @Test
    void 검색한_후보를_회원에게_귀속된_토큰으로_발급한다() {
        PlaceSourceCandidate source = source();
        PlaceCandidate candidate = PlaceCandidateFactory.create(source);
        when(placeSearchClient.search("건대 카페")).thenReturn(List.of(source));
        when(tokenManager.issue(10L, candidate)).thenReturn("signed-token");

        var response = placeSearchService.search(10L, "건대 카페");

        assertThat(response.places()).hasSize(1);
        assertThat(response.places().getFirst().selectionToken()).isEqualTo("signed-token");
        verify(tokenManager).issue(10L, candidate);
    }

    private PlaceSourceCandidate source() {
        return new PlaceSourceCandidate(
                PlaceProvider.NAVER,
                "클루버스 카페",
                "카페,디저트",
                "주소",
                "도로명 주소",
                new BigDecimal("37.1"),
                new BigDecimal("127.1"),
                null
        );
    }
}
