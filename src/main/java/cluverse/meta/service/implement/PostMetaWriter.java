package cluverse.meta.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.meta.domain.PostViewCount;
import cluverse.meta.exception.MetaExceptionMessage;
import cluverse.meta.repository.PostBookmarkCountRepository;
import cluverse.meta.repository.PostCommentCountRepository;
import cluverse.meta.repository.PostLikeCountRepository;
import cluverse.meta.repository.PostViewCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class PostMetaWriter {

    private final PostLikeCountRepository postLikeCountRepository;
    private final PostBookmarkCountRepository postBookmarkCountRepository;
    private final PostCommentCountRepository postCommentCountRepository;
    private final PostViewCountRepository postViewCountRepository;

    public void createViewCount(Long postId) {
        postViewCountRepository.save(PostViewCount.of(postId, 0));
    }

    public void increaseViewCount(Long postId) {
        postViewCountRepository.increaseCount(postId);
    }

    public void increaseLikeCount(Long postId) {
        postLikeCountRepository.increaseCount(postId);
    }

    public void increaseBookmarkCount(Long postId) {
        postBookmarkCountRepository.increaseCount(postId);
    }

    public void decreaseBookmarkCount(Long postId) {
        postBookmarkCountRepository.findByPostIdForUpdate(postId)
                .orElseThrow(() -> new BadRequestException(MetaExceptionMessage.POST_BOOKMARK_COUNT_ALREADY_ZERO.getMessage()));
        int updatedRowCount = postBookmarkCountRepository.decreaseCount(postId);
        validateUpdated(updatedRowCount, MetaExceptionMessage.POST_BOOKMARK_COUNT_ALREADY_ZERO);
        postBookmarkCountRepository.deleteIfZero(postId);
    }

    public void increaseCommentCount(Long postId) {
        postCommentCountRepository.increaseCount(postId);
    }

    public void decreaseCommentCount(Long postId) {
        postCommentCountRepository.findByPostIdForUpdate(postId)
                .orElseThrow(() -> new BadRequestException(MetaExceptionMessage.POST_COMMENT_COUNT_ALREADY_ZERO.getMessage()));
        int updatedRowCount = postCommentCountRepository.decreaseCount(postId);
        validateUpdated(updatedRowCount, MetaExceptionMessage.POST_COMMENT_COUNT_ALREADY_ZERO);
        postCommentCountRepository.deleteIfZero(postId);
    }

    private void validateUpdated(int updatedRowCount, MetaExceptionMessage message) {
        if (updatedRowCount == 0) {
            throw new BadRequestException(message.getMessage());
        }
    }
}
