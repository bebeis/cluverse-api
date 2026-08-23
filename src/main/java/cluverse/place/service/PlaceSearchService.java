package cluverse.place.service;

import cluverse.place.client.PlaceSearchClient;
import cluverse.place.service.implement.PlaceCandidateFactory;
import cluverse.place.service.implement.PlaceSelectionTokenManager;
import cluverse.place.service.response.PlaceSearchItemResponse;
import cluverse.place.service.response.PlaceSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceSearchService {

    private final PlaceSearchClient placeSearchClient;
    private final PlaceSelectionTokenManager tokenManager;

    public PlaceSearchResponse search(Long memberId, String query) {
        return new PlaceSearchResponse(placeSearchClient.search(query).stream()
                .map(PlaceCandidateFactory::create)
                .map(candidate -> PlaceSearchItemResponse.of(candidate, tokenManager.issue(memberId, candidate)))
                .toList());
    }
}
