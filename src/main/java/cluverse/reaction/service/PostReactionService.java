package cluverse.reaction.service;

import cluverse.reaction.service.response.PostBookmarkResponse;
import cluverse.reaction.service.response.PostLikeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PostReactionService {

    public PostLikeResponse likePost(Long memberId, Long postId) {
        throw unsupported();
    }

    public PostLikeResponse unlikePost(Long memberId, Long postId) {
        throw unsupported();
    }

    public PostBookmarkResponse bookmarkPost(Long memberId, Long postId) {
        throw unsupported();
    }

    public PostBookmarkResponse removeBookmark(Long memberId, Long postId) {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("반응 서비스는 아직 구현되지 않았습니다.");
    }
}
