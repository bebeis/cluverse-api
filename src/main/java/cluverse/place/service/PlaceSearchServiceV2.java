package cluverse.place.service;

import cluverse.place.client.PlaceSearchClient;
import cluverse.place.service.implement.PlaceCandidateFactory;
import cluverse.place.service.implement.PlaceSelectionTokenManager;
import cluverse.place.service.response.PlaceSearchItemResponseV2;
import cluverse.place.service.response.PlaceSearchResponseV2;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceSearchServiceV2 {

    private final PlaceSearchClient placeSearchClient;
    private final PlaceCandidateFactory placeCandidateFactory;
    private final PlaceSelectionTokenManager tokenManager;

    public PlaceSearchResponseV2 search(Long memberId, String query) {
        return new PlaceSearchResponseV2(placeSearchClient.search(query).stream()
                .map(placeCandidateFactory::create)
                .map(candidate -> PlaceSearchItemResponseV2.of(candidate, tokenManager.issue(memberId, candidate)))
                .toList());
    }
}
