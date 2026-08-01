package cluverse.post.service.implement;

import cluverse.board.service.implement.BoardReader;
import cluverse.member.service.implement.MemberReader;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.place.domain.Place;
import cluverse.place.domain.ResolvedPlaceAttachment;
import cluverse.place.domain.SelectedPlace;
import cluverse.place.service.implement.LocalPlaceAttachmentResolver;
import cluverse.place.service.implement.PlaceWriter;
import cluverse.post.domain.Post;
import cluverse.post.service.request.PostCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LocalMapPostWriteProcessorV2 {

    private final BoardReader boardReader;
    private final MemberReader memberReader;
    private final LocalPlaceAttachmentResolver attachmentResolver;
    private final PlaceWriter placeWriter;
    private final PostWriter postWriter;
    private final PostMetaWriter postMetaWriter;
    private final PostPlaceWriter postPlaceWriter;

    @Transactional
    public Long create(Long memberId, String requestId, PostCreateRequest request,
                       List<SelectedPlace> selectedPlaces, String clientIp) {
        boardReader.validateWritable(memberId, memberReader.isVerified(memberId), request.boardId());
        List<ResolvedPlaceAttachment> attachments = attachmentResolver.resolve(memberId, selectedPlaces);
        List<Place> places = placeWriter.upsertAll(
                attachments.stream().map(ResolvedPlaceAttachment::candidate).toList());

        Post post = postWriter.create(memberId, request, clientIp, requestId);
        postMetaWriter.createViewCount(post.getId());
        postPlaceWriter.createAll(post.getId(), attachments, places);
        return post.getId();
    }
}
