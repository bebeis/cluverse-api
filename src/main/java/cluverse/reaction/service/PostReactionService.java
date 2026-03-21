package cluverse.reaction.service;

import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.service.implement.PostAccessReader;
import cluverse.reaction.service.implement.PostReactionWriter;
import cluverse.reaction.service.response.PostBookmarkResponse;
import cluverse.reaction.service.response.PostLikeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PostReactionService {

    private final PostReactionWriter postReactionWriter;
    private final PostAccessReader postAccessReader;
    private final PostMetaWriter postMetaWriter;

    public PostLikeResponse likePost(Long memberId, Long postId) {
        postAccessReader.validateReadablePost(memberId, postId);
        postReactionWriter.likePost(memberId, postId);
        postMetaWriter.increaseLikeCount(postId);
        return PostLikeResponse.like(postId);
    }

    public PostBookmarkResponse bookmarkPost(Long memberId, Long postId) {
        postAccessReader.validateReadablePost(memberId, postId);
        postReactionWriter.bookmarkPost(memberId, postId);
        postMetaWriter.increaseBookmarkCount(postId);
        return PostBookmarkResponse.bookmark(postId);
    }

    public PostBookmarkResponse removeBookmark(Long memberId, Long postId) {
        postAccessReader.validateReadablePost(memberId, postId);
        postReactionWriter.removeBookmark(memberId, postId);
        postMetaWriter.decreaseBookmarkCount(postId);
        return PostBookmarkResponse.remove(postId);
    }
}
