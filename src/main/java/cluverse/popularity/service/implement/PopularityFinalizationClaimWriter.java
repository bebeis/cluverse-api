package cluverse.popularity.service.implement;

import cluverse.popularity.repository.PopularityFinalizationClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Transactional
public class PopularityFinalizationClaimWriter {

    private final PopularityFinalizationClaimRepository popularityFinalizationClaimRepository;

    public boolean claim(
            Long postId,
            String claimToken,
            LocalDateTime claimedAt,
            LocalDateTime staleBefore
    ) {
        popularityFinalizationClaimRepository.tryClaim(postId, claimToken, claimedAt, staleBefore);
        return popularityFinalizationClaimRepository.existsByPostIdAndClaimToken(postId, claimToken);
    }

    public void release(Long postId, String claimToken) {
        popularityFinalizationClaimRepository.release(postId, claimToken);
    }
}
