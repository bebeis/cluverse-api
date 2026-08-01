package cluverse.popularity.service.implement;

import cluverse.popularity.repository.PopularityCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Transactional
public class PopularityCandidateWriter {

    private final PopularityCandidateRepository popularityCandidateRepository;

    public void upsert(
            Long postId,
            Long boardId,
            LocalDateTime registeredAt,
            LocalDateTime nextCheckAt,
            LocalDateTime expiresAt
    ) {
        popularityCandidateRepository.upsert(postId, boardId, registeredAt, nextCheckAt, expiresAt);
    }

    public void reschedule(
            Long postId,
            Long boardId,
            LocalDateTime checkedAt,
            LocalDateTime nextCheckAt,
            LocalDateTime expiresAt
    ) {
        int updated = popularityCandidateRepository.reschedule(postId, checkedAt, nextCheckAt, expiresAt);
        if (updated == 0) {
            popularityCandidateRepository.upsert(postId, boardId, checkedAt, nextCheckAt, expiresAt);
        }
    }

    public void remove(Long postId) {
        popularityCandidateRepository.deleteById(postId);
    }
}
