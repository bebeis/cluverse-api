package cluverse.place.service.response;

import java.util.List;

public record PlaceSearchResponse(List<PlaceSearchItemResponse> places) {

    public PlaceSearchResponse {
        places = List.copyOf(places);
    }
}
