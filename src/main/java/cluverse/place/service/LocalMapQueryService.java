package cluverse.place.service;

import cluverse.place.domain.PlaceCategory;
import cluverse.place.service.implement.PlaceReader;
import cluverse.place.service.response.LocalMapMarkerResponse;
import cluverse.place.service.response.LocalMapResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalMapQueryService {

    private final PlaceReader placeReader;

    public LocalMapResponse read(Long universityId, Long campusId, PlaceCategory category) {
        return new LocalMapResponse(
                universityId,
                campusId,
                placeReader.readMarkers(universityId, campusId, category).stream()
                        .map(LocalMapMarkerResponse::from)
                        .toList()
        );
    }
}
