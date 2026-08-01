package cluverse.place.service.response;

import java.util.List;

public record PlaceSearchResponseV1(List<PlaceSearchItemResponseV1> places) {

    public PlaceSearchResponseV1 {
        places = List.copyOf(places);
    }
}
