package cluverse.reaction.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.reaction.domain.PostBookmark;
import cluverse.reaction.exception.PostReactionExceptionMessage;
import cluverse.reaction.repository.PostBookmarkRepository;
import cluverse.reaction.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class PostReactionWriter {

    private final PostLikeRepository postLikeRepository;
    private final PostBookmarkRepository postBookmarkRepository;

    public void likePost(Long memberId, Long postId) {
        validateNotAlreadyLiked(memberId, postId);
        postLikeRepository.save(cluverse.reaction.domain.PostLike.of(postId, memberId));
    }

    public void bookmarkPost(Long memberId, Long postId) {
        validateNotAlreadyBookmarked(memberId, postId);
        postBookmarkRepository.save(PostBookmark.of(memberId, postId));
    }

    public void removeBookmark(Long memberId, Long postId) {
        PostBookmark postBookmark = postBookmarkRepository.findByMemberIdAndPostId(memberId, postId)
                .orElseThrow(() -> new BadRequestException(PostReactionExceptionMessage.POST_NOT_BOOKMARKED.getMessage()));
        postBookmarkRepository.delete(postBookmark);
    }

    private void validateNotAlreadyLiked(Long memberId, Long postId) {
        if (postLikeRepository.existsByPostIdAndMemberId(postId, memberId)) {
            throw new BadRequestException(PostReactionExceptionMessage.POST_ALREADY_LIKED.getMessage());
        }
    }

    private void validateNotAlreadyBookmarked(Long memberId, Long postId) {
        if (postBookmarkRepository.existsByMemberIdAndPostId(memberId, postId)) {
            throw new BadRequestException(PostReactionExceptionMessage.POST_ALREADY_BOOKMARKED.getMessage());
        }
    }
}
