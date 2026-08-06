package cluverse.post.service.implement;

import cluverse.place.domain.SelectedPlace;

import java.util.List;

public record PostPlaceVerificationRequested(
        Long memberId,
        Long postId,
        List<SelectedPlace> places
) {
    public PostPlaceVerificationRequested {
        places = List.copyOf(places);
    }
}
