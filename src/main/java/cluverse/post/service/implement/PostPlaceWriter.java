package cluverse.post.service.implement;

import cluverse.place.domain.Place;
import cluverse.place.domain.ResolvedPlaceAttachment;
import cluverse.post.domain.Post;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostPlaceWriter {

    public void createAll(Post post, List<ResolvedPlaceAttachment> attachments, List<Place> places) {
        for (int index = 0; index < attachments.size(); index++) {
            ResolvedPlaceAttachment attachment = attachments.get(index);
            post.addPlace(
                    places.get(index).getId(), index, attachment.authorUniversityId(),
                    attachment.universityCampusId(), attachment.recommended()
            );
        }
    }
}
