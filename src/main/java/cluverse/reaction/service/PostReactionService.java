package cluverse.reaction.service;

import cluverse.meta.service.PostMetaService;
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
    private final PostMetaService postMetaService;
    private final PostService postService;

    public PostLikeResponse likePost(Long memberId, Long postId) {
        postService.validateReadablePost(memberId, postId);
        postReactionWriter.likePost(memberId, postId);
        postMetaService.increaseLikeCount(postId);
        return PostLikeResponse.like(postId);
    }

    public PostBookmarkResponse bookmarkPost(Long memberId, Long postId) {
        postService.validateReadablePost(memberId, postId);
        postReactionWriter.bookmarkPost(memberId, postId);
        postMetaService.increaseBookmarkCount(postId);
        return PostBookmarkResponse.bookmark(postId);
    }

    public PostBookmarkResponse removeBookmark(Long memberId, Long postId) {
        postService.validateReadablePost(memberId, postId);
        postReactionWriter.removeBookmark(memberId, postId);
        postMetaService.decreaseBookmarkCount(postId);
        return PostBookmarkResponse.remove(postId);
    }
}
