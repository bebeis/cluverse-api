package cluverse.comment.service;

import cluverse.comment.repository.CommentQueryRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService {

    private final CommentReader commentReader;
    private final CommentQueryRepository commentQueryRepository;
    private final PostAccessReader postAccessReader;

    public CommentPageResponse getComments(Long memberId, CommentPageRequest request) {
        postAccessReader.validateReadablePost(memberId, request.postId());
        validateParentComment(request.postId(), request.parentCommentId());

        CommentPageQueryResult queryResult = commentQueryRepository.findCommentPage(memberId, request);
        List<CommentResponse> comments = queryResult.comments().stream()
                .map(comment -> CommentResponse.from(comment, memberId))
                .toList();

        return new CommentPageResponse(comments, request.offset(), request.limit(), queryResult.hasNext());
    }

    public CommentReactionTargetResponse getReactionTarget(Long commentId) {
        return commentReader.readReactionTarget(commentId);
    }

    public CommentResponse getComment(Long memberId, Long commentId) {
        return CommentResponse.from(commentQueryRepository.findComment(memberId, commentId), memberId);
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
}
