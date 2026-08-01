package cluverse.place.service.response;

import java.util.List;

public record LocalMapResponse(
        Long universityId,
        Long campusId,
        List<LocalMapMarkerResponse> places
) {
    public LocalMapResponse {
        places = List.copyOf(places);
    }
}
