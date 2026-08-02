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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentQueryService {

    private final CommentReader commentReader;
    private final PostAccessReader postAccessReader;

    public CommentPageResponse getCommentsV1(Long memberId, CommentPageRequest request) {
        return getComments(memberId, request, false);
    }

    public CommentPageResponse getCommentsV2(Long memberId, CommentPageRequest request) {
        return getComments(memberId, request, true);
    }

    private CommentPageResponse getComments(Long memberId, CommentPageRequest request, boolean usePersistedPath) {
        postAccessReader.validateReadablePost(memberId, request.postId());
        validateParentComment(request.postId(), request.parentCommentId());

        CommentPageCursor cursor = resolveCursor(request.cursor());
        CommentPageQueryResult queryResult = usePersistedPath
                ? commentReader.readCommentPageV2(memberId, request, cursor)
                : commentReader.readCommentPageV1(memberId, request, cursor);
        List<CommentResponse> comments = queryResult.comments().stream()
                .map(comment -> CommentResponse.from(comment, memberId))
                .toList();

        String nextCursor = createNextCursor(cursor, queryResult);
        return new CommentPageResponse(comments, nextCursor, request.limit(), queryResult.hasNext());
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
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
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
}
