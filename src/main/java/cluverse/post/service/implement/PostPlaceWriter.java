package cluverse.post.service.implement;

import cluverse.place.domain.Place;
import cluverse.place.domain.ResolvedPlaceAttachment;
import cluverse.post.domain.PostPlace;
import cluverse.post.repository.PostPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
public class PostPlaceWriter {

    private final PostPlaceRepository postPlaceRepository;

    public void createAll(Long postId, List<ResolvedPlaceAttachment> attachments, List<Place> places) {
        List<PostPlace> postPlaces = new ArrayList<>();
        for (int index = 0; index < attachments.size(); index++) {
            ResolvedPlaceAttachment attachment = attachments.get(index);
            postPlaces.add(PostPlace.of(
                    postId, places.get(index).getId(), index, attachment.authorUniversityId(),
                    attachment.universityCampusId(), attachment.recommended()
            ));
        }
        postPlaceRepository.saveAll(postPlaces);
    }
}
