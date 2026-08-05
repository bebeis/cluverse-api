package cluverse.popularity.service;

import cluverse.popularity.domain.PopularPostSortType;
import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.service.implement.PopularPostReader;
import cluverse.popularity.service.implement.PopularPostView;
import cluverse.popularity.service.response.PopularPostListResponse;
import cluverse.popularity.service.response.PopularPostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopularPostQueryService {

    private final PopularPostReader popularPostReader;

    public PopularPostListResponse getRecent(PopularityAlgorithmVersion version, int size) {
        return new PopularPostListResponse(
                version,
                PopularPostSortType.LATEST,
                popularPostReader.readRecent(version, size).stream()
                        .map(summary -> toResponse(summary))
                        .toList()
        );
    }

    public PopularPostListResponse getHistory(
            PopularityAlgorithmVersion version,
            PopularPostSortType sort,
            int size
    ) {
        return new PopularPostListResponse(
                version,
                sort,
                popularPostReader.readHistory(version, sort, size).stream()
                        .map(summary -> toResponse(summary))
                        .toList()
        );
    }

    private PopularPostResponse toResponse(PopularPostView summary) {
        return PopularPostResponse.of(
                summary.postId(),
                summary.boardId(),
                summary.title(),
                summary.score(),
                summary.likeCount(),
                summary.commentCount(),
                summary.promotedAt(),
                summary.finalizedAt()
        );
    }
}
