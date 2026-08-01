package cluverse.place.service.response;

import java.util.List;

public record PlaceSearchResponseV2(List<PlaceSearchItemResponseV2> places) {

    public PlaceSearchResponseV2 {
        places = List.copyOf(places);
    }
}
