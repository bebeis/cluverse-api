package cluverse.reaction.service;

import cluverse.post.service.PostService;
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
    private final PostService postService;

    public PostLikeResponse likePost(Long memberId, Long postId) {
        postReactionWriter.likePost(memberId, postId);
        postService.increaseLikeCount(postId);
        return PostLikeResponse.like(postId);
    }

    public PostLikeResponse unlikePost(Long memberId, Long postId) {
        postReactionWriter.unlikePost(memberId, postId);
        postService.decreaseLikeCount(postId);
        return PostLikeResponse.unlike(postId);
    }

    public PostBookmarkResponse bookmarkPost(Long memberId, Long postId) {
        postReactionWriter.bookmarkPost(memberId, postId);
        postService.increaseBookmarkCount(postId);
        return PostBookmarkResponse.bookmark(postId);
    }

    public PostBookmarkResponse removeBookmark(Long memberId, Long postId) {
        postReactionWriter.removeBookmark(memberId, postId);
        postService.decreaseBookmarkCount(postId);
        return PostBookmarkResponse.remove(postId);
    }
}
