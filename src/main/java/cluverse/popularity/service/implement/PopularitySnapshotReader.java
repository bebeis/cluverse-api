package cluverse.popularity.service.implement;

import cluverse.popularity.repository.PopularityQueryRepository;
import cluverse.popularity.repository.dto.PopularitySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PopularitySnapshotReader {

    private final PopularityQueryRepository popularityQueryRepository;

    public PopularitySnapshot read(Long postId) {
        return popularityQueryRepository.findSnapshot(postId).orElse(null);
    }

    public List<PopularitySnapshot> readAll(List<Long> postIds) {
        return popularityQueryRepository.findSnapshots(postIds);
    }

    public List<PopularitySnapshot> readRecentAfter(
            LocalDateTime createdFrom,
            LocalDateTime lastCreatedAt,
            long lastPostId,
            int limit
    ) {
        return popularityQueryRepository.findRecentSnapshotsAfter(
                createdFrom,
                lastCreatedAt,
                lastPostId,
                limit
        );
    }
}
