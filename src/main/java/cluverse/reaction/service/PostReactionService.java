package cluverse.reaction.service;

import cluverse.popularity.domain.PopularityTrigger;
import cluverse.popularity.service.implement.PopularityPromotionInvoker;
import cluverse.reaction.service.implement.PostReactionProcessor;
import cluverse.reaction.service.response.PostBookmarkResponse;
import cluverse.reaction.service.response.PostLikeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostReactionService {

    private final PostReactionProcessor postReactionProcessor;
    private final PopularityPromotionInvoker popularityPromotionInvoker;

    public PostLikeResponse likePost(Long memberId, Long postId) {
        postReactionProcessor.likePost(memberId, postId);
        popularityPromotionInvoker.tryEvaluate(postId, PopularityTrigger.LIKE);
        return PostLikeResponse.like(postId);
    }

    public PostBookmarkResponse bookmarkPost(Long memberId, Long postId) {
        postReactionProcessor.bookmarkPost(memberId, postId);
        return PostBookmarkResponse.bookmark(postId);
    }

    public PostBookmarkResponse removeBookmark(Long memberId, Long postId) {
        postReactionProcessor.removeBookmark(memberId, postId);
        return PostBookmarkResponse.remove(postId);
    }
}
