package cluverse.comment.service.implement;

import cluverse.comment.domain.Comment;
import cluverse.comment.service.request.CommentCreateRequest;
import cluverse.place.domain.Place;
import cluverse.place.domain.ResolvedPlaceAttachment;
import cluverse.place.domain.SelectedPlace;
import cluverse.place.service.implement.LocalPlaceAttachmentResolver;
import cluverse.place.service.implement.PlaceWriter;
import cluverse.post.service.implement.PostAccessReader;
import cluverse.meta.service.implement.PostMetaWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LocalMapCommentWriteProcessorV2 {

    private final PostAccessReader postAccessReader;
    private final CommentReader commentReader;
    private final CommentWriter commentWriter;
    private final LocalPlaceAttachmentResolver attachmentResolver;
    private final PlaceWriter placeWriter;
    private final CommentPlaceWriter commentPlaceWriter;
    private final PostMetaWriter postMetaWriter;

    @Transactional
    public Long create(Long memberId, Long postId, String requestId, CommentCreateRequest request,
                       SelectedPlace selectedPlace, String clientIp) {
        postAccessReader.validateWritablePost(memberId, postId);
        ResolvedPlaceAttachment attachment = attachmentResolver.resolve(memberId, List.of(selectedPlace)).getFirst();
        Place place = placeWriter.upsert(attachment.candidate());
        Comment parent = resolveParent(postId, request.parentCommentId());
        Comment comment = commentWriter.create(memberId, postId, parent, request, clientIp, requestId);
        postMetaWriter.increaseCommentCount(postId);
        if (parent != null) {
            commentWriter.increaseReplyCount(parent.getId());
        }
        commentPlaceWriter.create(comment.getId(), attachment, place);
        return comment.getId();
    }

    private Comment resolveParent(Long postId, Long parentCommentId) {
        if (parentCommentId == null) {
            return null;
        }
        Comment parent = commentReader.readOrThrow(parentCommentId);
        commentReader.validateBelongsToPost(parent, postId);
        commentReader.validateReplyWritable(parent);
        return parent;
    }
}
