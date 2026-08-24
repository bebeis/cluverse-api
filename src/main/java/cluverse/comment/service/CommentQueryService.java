package cluverse.comment.service;

import cluverse.comment.domain.CommentPageCursor;
import cluverse.comment.repository.dto.CommentPageQueryResult;
import cluverse.comment.service.implement.CommentReader;
import cluverse.comment.service.request.CommentPageRequest;
import cluverse.comment.service.response.CommentLastRepliedPost;
import cluverse.comment.service.response.CommentPageResponse;
import cluverse.comment.service.response.CommentReactionTargetResponse;
import cluverse.comment.service.response.CommentResponse;
import cluverse.post.service.implement.PostAccessReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentQueryService {

    private final CommentReader commentReader;
    private final PostAccessReader postAccessReader;
    private final Clock clock;

    public CommentPageResponse getComments(Long memberId, CommentPageRequest request) {
        postAccessReader.validateReadablePost(memberId, request.postId());
        validateParentComment(request.postId(), request.parentCommentId());

        CommentPageCursor cursor = resolveCursor(request.cursor());
        CommentPageQueryResult queryResult = commentReader.readCommentPage(memberId, request, cursor);
        return toResponse(memberId, request.limit(), cursor, queryResult);
    }

    public CommentPageResponse getThread(Long memberId, Long rootCommentId, CommentPageRequest request) {
        postAccessReader.validateReadablePost(memberId, request.postId());
        cluverse.comment.domain.Comment root = commentReader.readOrThrow(rootCommentId);
        commentReader.validateBelongsToPost(root, request.postId());

        CommentPageCursor cursor = resolveCursor(request.cursor());
        CommentPageQueryResult queryResult = commentReader.readCommentThreadPage(
                memberId, request.postId(), rootCommentId, cursor, request.limit());
        return toResponse(memberId, request.limit(), cursor, queryResult);
    }

    public CommentReactionTargetResponse getReactionTarget(Long commentId) {
        return commentReader.readReactionTarget(commentId);
    }

    public CommentResponse getComment(Long memberId, Long commentId) {
        return CommentResponse.from(commentReader.readComment(memberId, commentId), memberId);
    }

    public List<CommentLastRepliedPost> getRecentCommentRepliedPostIds(final Long size) {
        return commentReader.readRecentCommentRepliedPosts(size);
    }

    private void validateParentComment(Long postId, Long parentCommentId) {
        if (parentCommentId == null) {
            return;
        }
        cluverse.comment.domain.Comment parentComment = commentReader.readOrThrow(parentCommentId);
        commentReader.validateBelongsToPost(parentComment, postId);
    }

    private CommentPageCursor resolveCursor(String encodedCursor) {
        if (encodedCursor != null) {
            return CommentPageCursor.decode(encodedCursor);
        }
        return CommentPageCursor.first(
                LocalDateTime.ofInstant(clock.instant(), clock.getZone()),
                commentReader.readMaxCommentId()
        );
    }

    private String createNextCursor(CommentPageCursor cursor, CommentPageQueryResult queryResult) {
        if (!queryResult.hasNext() || queryResult.lastPath() == null) {
            return null;
        }
        return new CommentPageCursor(
                queryResult.lastPath(),
                cursor.asOf(),
                cursor.snapshotMaxCommentId()
        ).encode();
    }

    private CommentPageResponse toResponse(
            Long memberId,
            int limit,
            CommentPageCursor cursor,
            CommentPageQueryResult queryResult
    ) {
        List<CommentResponse> comments = queryResult.comments().stream()
                .map(comment -> CommentResponse.from(comment, memberId))
                .toList();
        return new CommentPageResponse(
                comments,
                createNextCursor(cursor, queryResult),
                limit,
                queryResult.hasNext()
        );
    }
}
