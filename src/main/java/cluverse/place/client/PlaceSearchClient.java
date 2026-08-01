package cluverse.place.client;

import cluverse.place.domain.PlaceSourceCandidate;

import java.util.List;

public interface PlaceSearchClient {

    List<PlaceSourceCandidate> search(String query);
}
