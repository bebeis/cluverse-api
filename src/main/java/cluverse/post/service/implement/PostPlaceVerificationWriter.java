package cluverse.post.service.implement;

import cluverse.post.domain.PostPlaceVerification;
import cluverse.post.repository.PostPlaceVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PostPlaceVerificationWriter {

    private final PostPlaceVerificationRepository repository;

    @Transactional
    public void start(Long postId) {
        repository.save(PostPlaceVerification.pending(postId));
    }

    @Transactional
    public void complete(Long postId) {
        read(postId).complete();
    }

    @Transactional
    public void fail(Long postId, String reason) {
        read(postId).fail(reason);
    }

    @Transactional(readOnly = true)
    public PostPlaceVerification readOrNull(Long postId) {
        return repository.findById(postId).orElse(null);
    }

    private PostPlaceVerification read(Long postId) {
        return repository.findById(postId)
                .orElseThrow(() -> new IllegalStateException("장소 검증 상태를 찾을 수 없습니다."));
    }
}
