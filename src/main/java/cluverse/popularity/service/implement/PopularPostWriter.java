package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.domain.PopularityTrigger;
import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.PopularPostRepository;
import cluverse.popularity.repository.dto.PopularitySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Transactional
public class PopularPostWriter {

    private final PopularPostRepository popularPostRepository;
    private final PopularityProperties properties;
    private final Clock clock;

    public void promote(
            PopularityAlgorithmVersion version,
            PopularitySnapshot snapshot,
            PopularityPolicy policy,
            PopularityTrigger trigger
    ) {
        LocalDateTime promotedAt = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
        popularPostRepository.upsertPromotion(
                version,
                snapshot.postId(),
                snapshot.boardId(),
                promotedAt,
                snapshot.createdAt().plus(properties.promotionWindow()),
                score(snapshot),
                trigger,
                policy.promotionScore(),
                policy.likeGate(),
                policy.commentGate()
        );
    }

    public boolean finalizeSnapshot(Long popularPostId, PopularitySnapshot snapshot, LocalDateTime finalizedAt) {
        return popularPostRepository.finalizeIfPending(
                popularPostId,
                score(snapshot),
                snapshot.likeCount(),
                snapshot.commentCount(),
                snapshot.viewCount(),
                finalizedAt
        ) == 1;
    }

    private long score(PopularitySnapshot snapshot) {
        return snapshot.likeCount() * properties.scoreLikeWeight()
                + snapshot.commentCount() * properties.scoreCommentWeight()
                + snapshot.viewCount() * properties.scoreViewWeight();
    }
}
