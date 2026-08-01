package cluverse.comment.service.implement;

import cluverse.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalMapCommentReader {

    private final CommentRepository commentRepository;

    public Optional<Long> findIdByRequestId(Long memberId, String requestId) {
        return commentRepository.findByMemberIdAndClientRequestId(memberId, requestId)
                .map(comment -> comment.getId());
    }
}
