package cluverse.place.service;

import cluverse.place.client.PlaceSearchClient;
import cluverse.place.service.implement.PlaceCandidateFactory;
import cluverse.place.service.response.PlaceSearchItemResponseV1;
import cluverse.place.service.response.PlaceSearchResponseV1;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceSearchServiceV1 {

    private final PlaceSearchClient placeSearchClient;
    private final PlaceCandidateFactory placeCandidateFactory;

    public PlaceSearchResponseV1 search(String query) {
        return new PlaceSearchResponseV1(placeSearchClient.search(query).stream()
                .map(placeCandidateFactory::create)
                .map(candidate -> PlaceSearchItemResponseV1.of(query, candidate))
                .toList());
    }
}
