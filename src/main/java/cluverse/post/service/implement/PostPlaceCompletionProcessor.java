package cluverse.post.service.implement;

import cluverse.place.domain.Place;
import cluverse.place.domain.ResolvedPlaceAttachment;
import cluverse.place.domain.SelectedPlace;
import cluverse.place.service.implement.LocalPlaceAttachmentResolver;
import cluverse.place.service.implement.PlaceWriter;
import cluverse.post.domain.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PostPlaceCompletionProcessor {

    private final PostAccessReader postAccessReader;
    private final LocalPlaceAttachmentResolver attachmentResolver;
    private final PlaceWriter placeWriter;
    private final PostPlaceVerificationWriter verificationWriter;

    @Transactional
    public void complete(Long memberId, Long postId, List<SelectedPlace> selectedPlaces) {
        List<ResolvedPlaceAttachment> attachments = attachmentResolver.resolve(memberId, selectedPlaces);
        List<Place> places = placeWriter.upsertAll(
                attachments.stream().map(ResolvedPlaceAttachment::candidate).toList());
        Post post = postAccessReader.readWithPlacesOrThrow(postId);

        for (int index = 0; index < attachments.size(); index++) {
            ResolvedPlaceAttachment attachment = attachments.get(index);
            post.addPlace(
                    places.get(index).getId(), index, attachment.authorUniversityId(),
                    attachment.universityCampusId(), attachment.recommended()
            );
        }
        verificationWriter.complete(postId);
    }
}
