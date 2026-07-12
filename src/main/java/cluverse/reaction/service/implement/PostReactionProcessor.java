package cluverse.reaction.service.implement;

import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.service.implement.PostAccessReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class PostReactionProcessor {

    private final PostReactionWriter postReactionWriter;
    private final PostAccessReader postAccessReader;
    private final PostMetaWriter postMetaWriter;

    public void likePost(Long memberId, Long postId) {
        postAccessReader.validateReadablePost(memberId, postId);
        postReactionWriter.likePost(memberId, postId);
        postMetaWriter.increaseLikeCount(postId);
    }

    public void bookmarkPost(Long memberId, Long postId) {
        postAccessReader.validateReadablePost(memberId, postId);
        postReactionWriter.bookmarkPost(memberId, postId);
        postMetaWriter.increaseBookmarkCount(postId);
    }

    public void removeBookmark(Long memberId, Long postId) {
        postAccessReader.validateReadablePost(memberId, postId);
        postReactionWriter.removeBookmark(memberId, postId);
        postMetaWriter.decreaseBookmarkCount(postId);
    }
}
