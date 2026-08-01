package cluverse.comment.service.implement;

import cluverse.comment.domain.CommentPlace;
import cluverse.comment.repository.CommentPlaceRepository;
import cluverse.place.domain.Place;
import cluverse.place.domain.ResolvedPlaceAttachment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CommentPlaceWriter {

    private final CommentPlaceRepository commentPlaceRepository;

    public void create(Long commentId, ResolvedPlaceAttachment attachment, Place place) {
        commentPlaceRepository.save(CommentPlace.of(
                commentId, place.getId(), attachment.authorUniversityId(),
                attachment.universityCampusId(), attachment.recommended()
        ));
    }
}
