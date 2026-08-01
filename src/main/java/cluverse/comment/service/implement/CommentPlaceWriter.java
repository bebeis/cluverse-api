package cluverse.comment.service.implement;

import cluverse.comment.domain.Comment;
import cluverse.place.domain.Place;
import cluverse.place.domain.ResolvedPlaceAttachment;
import org.springframework.stereotype.Component;

@Component
public class CommentPlaceWriter {

    public void create(Comment comment, ResolvedPlaceAttachment attachment, Place place) {
        comment.attachPlace(
                place.getId(), attachment.authorUniversityId(),
                attachment.universityCampusId(), attachment.recommended());
    }
}
