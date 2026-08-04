package cluverse.comment.service.implement;

import cluverse.comment.domain.Comment;
import cluverse.comment.service.request.CommentCreateRequest;
import cluverse.comment.service.request.CommentWithPlaceCreateRequestV1;
import cluverse.member.service.implement.MemberReader;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.place.domain.Place;
import cluverse.place.domain.ResolvedPlaceAttachment;
import cluverse.place.domain.SelectedPlace;
import cluverse.place.service.implement.LocalPlaceAttachmentResolver;
import cluverse.place.service.implement.PlaceWriter;
import cluverse.place.service.implement.V1PlaceSelectionResolver;
import cluverse.post.service.implement.PostAccessReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LocalMapCommentWriteProcessorV1 {

    private final PostAccessReader postAccessReader;
    private final MemberReader memberReader;
    private final CommentReader commentReader;
    private final CommentWriter commentWriter;
    private final V1PlaceSelectionResolver placeSelectionResolver;
    private final LocalPlaceAttachmentResolver attachmentResolver;
    private final PlaceWriter placeWriter;
    private final PostMetaWriter postMetaWriter;
    private final PostCommentActivityWriter postCommentActivityWriter;

    @Transactional
    public Long create(Long memberId, Long postId, CommentWithPlaceCreateRequestV1 request, String clientIp) {
        postAccessReader.validateWritablePost(memberId, postId);
        memberReader.isVerified(memberId);
        List<SelectedPlace> selected = placeSelectionResolver.resolve(List.of(request.place()));
        ResolvedPlaceAttachment attachment = attachmentResolver.resolve(memberId, selected).getFirst();
        Place place = placeWriter.upsert(attachment.candidate());
        return createComment(memberId, postId, request.comment(), clientIp, attachment, place, null);
    }

    private Long createComment(Long memberId, Long postId, CommentCreateRequest request, String clientIp,
                               ResolvedPlaceAttachment attachment, Place place, String requestId) {
        Comment parent = resolveParent(postId, request.parentCommentId());
        Comment comment = requestId == null
                ? commentWriter.create(memberId, postId, parent, request, clientIp)
                : commentWriter.create(memberId, postId, parent, request, clientIp, requestId);
        postMetaWriter.increaseCommentCount(postId);
        postCommentActivityWriter.reflectCreated(comment);
        if (parent != null) {
            commentWriter.increaseReplyCount(parent.getId());
        }
        comment.attachPlace(
                place.getId(), attachment.authorUniversityId(),
                attachment.universityCampusId(), attachment.recommended());
        return comment.getId();
    }

    private Comment resolveParent(Long postId, Long parentCommentId) {
        if (parentCommentId == null) {
            return null;
        }
        Comment parent = commentReader.readForUpdateOrThrow(parentCommentId);
        commentReader.validateBelongsToPost(parent, postId);
        commentReader.validateReplyWritable(parent);
        return parent;
    }
}
