package cluverse.post.service.implement;

import cluverse.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalMapPostReader {

    private final PostRepository postRepository;

    public Optional<Long> findIdByRequestId(Long memberId, String requestId) {
        return postRepository.findByMemberIdAndClientRequestId(memberId, requestId).map(post -> post.getId());
    }
}
