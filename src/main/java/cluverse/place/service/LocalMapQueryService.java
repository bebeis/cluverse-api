package cluverse.place.service;

import cluverse.place.domain.PlaceCategory;
import cluverse.place.service.implement.LocalMapReader;
import cluverse.place.service.response.LocalMapMarkerResponse;
import cluverse.place.service.response.LocalMapResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalMapQueryService {

    private final LocalMapReader localMapReader;

    public LocalMapResponse read(Long universityId, Long campusId, PlaceCategory category) {
        return new LocalMapResponse(
                universityId,
                campusId,
                localMapReader.readMarkers(universityId, campusId, category).stream()
                        .map(LocalMapMarkerResponse::from)
                        .toList()
        );
    }
}
