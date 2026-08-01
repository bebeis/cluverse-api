package cluverse.place.domain;

public record ResolvedPlaceAttachment(
        PlaceCandidate candidate,
        Long authorUniversityId,
        Long universityCampusId,
        boolean recommended
) {
}
