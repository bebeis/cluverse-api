package cluverse.popularity.service.implement;

import cluverse.popularity.domain.BoardPopularityPolicy;
import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.BoardPopularityPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PopularityPolicyReader {

    private final BoardPopularityPolicyRepository boardPopularityPolicyRepository;
    private final PopularityPolicyCache popularityPolicyCache;
    private final PopularityProperties properties;

    public PopularityPolicy read(Long boardId) {
        return popularityPolicyCache.get(boardId).orElseGet(() -> readAndCache(boardId));
    }

    private PopularityPolicy readAndCache(Long boardId) {
        PopularityPolicy policy = boardPopularityPolicyRepository.findById(boardId)
                .map(this::toPolicy)
                .orElseGet(this::defaultPolicy);
        popularityPolicyCache.put(boardId, policy);
        return policy;
    }

    private PopularityPolicy toPolicy(BoardPopularityPolicy policy) {
        return new PopularityPolicy(
                policy.getPromotionScore()
        );
    }

    private PopularityPolicy defaultPolicy() {
        return new PopularityPolicy(
                properties.defaultPromotionScore()
        );
    }
}
