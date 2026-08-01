package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularityCandidate;
import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.PopularityCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PopularityCandidateClaimer {

    private final PopularityCandidateRepository popularityCandidateRepository;
    private final PopularityProperties properties;

    @Transactional
    public List<PopularityCandidateClaim> claimDue(LocalDateTime now) {
        List<PopularityCandidate> candidates = popularityCandidateRepository.findDueForUpdate(
                now,
                properties.candidateBatchSize()
        );
        LocalDateTime nextCheckAt = now.plus(properties.candidateRecheckInterval());
        for (PopularityCandidate candidate : candidates) {
            popularityCandidateRepository.reschedule(
                    candidate.getPostId(),
                    now,
                    nextCheckAt,
                    candidate.getExpiresAt()
            );
        }
        return candidates.stream()
                .map(candidate -> new PopularityCandidateClaim(
                        candidate.getPostId(),
                        candidate.getNextCheckAt()
                ))
                .toList();
    }
}
