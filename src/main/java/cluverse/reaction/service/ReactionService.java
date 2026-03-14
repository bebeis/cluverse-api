package cluverse.reaction.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReactionService {

    public void likePost(Long memberId, Long postId) {
        throw unsupported();
    }

    public void unlikePost(Long memberId, Long postId) {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("반응 서비스는 아직 구현되지 않았습니다.");
    }
}
