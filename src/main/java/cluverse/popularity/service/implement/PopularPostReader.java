package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularPostSortType;
import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.repository.PopularityQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PopularPostReader {

    private final PopularityQueryRepository popularityQueryRepository;

    public List<PopularPostView> readRecent(PopularityAlgorithmVersion version, int size) {
        return read(version, false, PopularPostSortType.LATEST, size);
    }

    public List<PopularPostView> readHistory(
            PopularityAlgorithmVersion version,
            PopularPostSortType sort,
            int size
    ) {
        return read(version, true, sort, size);
    }

    private List<PopularPostView> read(
            PopularityAlgorithmVersion version,
            boolean finalized,
            PopularPostSortType sort,
            int size
    ) {
        return popularityQueryRepository.findPopularPosts(version, finalized, sort, size).stream()
                .map(summary -> new PopularPostView(
                        summary.postId(),
                        summary.boardId(),
                        summary.title(),
                        summary.score(),
                        summary.likeCount(),
                        summary.commentCount(),
                        summary.promotedAt(),
                        summary.finalizedAt()
                ))
                .toList();
    }
}
