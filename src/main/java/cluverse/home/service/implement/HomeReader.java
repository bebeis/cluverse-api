package cluverse.home.service.implement;

import cluverse.home.properties.HomeRecentCommentProperties;
import cluverse.home.repository.HomeQueryRepository;
import cluverse.home.repository.dto.AccessiblePostQueryResult;
import cluverse.home.repository.dto.HomeBoardQueryResult;
import cluverse.home.repository.dto.RecentCommentedPostCandidateQueryResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Component
@Transactional(readOnly = true)
public class HomeReader {

    private static final String RECENT_COMMENT_SNAPSHOT_KEY = "global";

    private final HomeQueryRepository homeQueryRepository;
    private final Cache<String, RecentCommentSnapshot> recentCommentSnapshotCache;
    private final int snapshotCandidateSize;
    private final Counter snapshotCacheHit;
    private final Counter snapshotCacheMiss;
    private final Counter snapshotFallback;

    public HomeReader(
            HomeQueryRepository homeQueryRepository,
            HomeRecentCommentProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.homeQueryRepository = homeQueryRepository;
        this.recentCommentSnapshotCache = Caffeine.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(properties.snapshotCacheTtl())
                .build();
        this.snapshotCandidateSize = properties.snapshotCandidateSize();
        this.snapshotCacheHit = cacheCounter(meterRegistry, "hit");
        this.snapshotCacheMiss = cacheCounter(meterRegistry, "miss");
        this.snapshotFallback = Counter.builder("home.recent.comment.snapshot.fallback")
                .description("최근 댓글 후보 스냅숏 범위를 넘어 원본 집계로 폴백한 횟수")
                .register(meterRegistry);
    }

    public FavoriteBoardPageView readFavoriteBoards(Long memberId, Long cursorBoardId, int size) {
        List<HomeBoardQueryResult> rows = homeQueryRepository.findFavoriteBoards(
                memberId, cursorBoardId, size + 1
        );
        boolean hasNext = rows.size() > size;
        List<FavoriteBoardView> boards = rows.stream()
                .limit(size)
                .map(FavoriteBoardView::from)
                .toList();
        Long nextCursor = hasNext ? boards.getLast().boardId() : null;
        return new FavoriteBoardPageView(boards, nextCursor, hasNext);
    }

    public List<RecentCommentedPostView> readRecentCommentedPostsV1(Long memberId, int size) {
        return homeQueryRepository.findRecentCommentedPostsV1(memberId, size).stream()
                .map(RecentCommentedPostView::from)
                .toList();
    }

    public List<RecentCommentedPostView> readRecentCommentedPostsV2(Long memberId, int size) {
        RecentCommentSnapshot snapshot = readRecentCommentSnapshot();
        if (snapshot.candidates().isEmpty()) {
            return List.of();
        }

        Map<Long, AccessiblePostQueryResult> accessiblePosts = homeQueryRepository.findAccessiblePostTitles(
                        memberId,
                        snapshot.candidates().stream()
                                .map(RecentCommentedPostCandidateQueryResult::postId)
                                .toList()
                ).stream()
                .collect(toMap(AccessiblePostQueryResult::postId, identity()));
        List<RecentCommentedPostView> result = snapshot.candidates().stream()
                .filter(candidate -> accessiblePosts.containsKey(candidate.postId()))
                .limit(size)
                .map(candidate -> new RecentCommentedPostView(
                        candidate.postId(),
                        accessiblePosts.get(candidate.postId()).title(),
                        candidate.lastCommentedAt()
                ))
                .toList();
        if (result.size() >= size || !snapshot.hasMore()) {
            return result;
        }

        snapshotFallback.increment();
        return homeQueryRepository.findRecentCommentedPostsV2Fallback(memberId, size).stream()
                .map(RecentCommentedPostView::from)
                .toList();
    }

    public List<RecentCommentedPostView> readRecentCommentedPostsV3(Long memberId, int size) {
        return homeQueryRepository.findRecentCommentedPostsV3(memberId, size).stream()
                .map(RecentCommentedPostView::from)
                .toList();
    }

    private RecentCommentSnapshot readRecentCommentSnapshot() {
        RecentCommentSnapshot snapshot = recentCommentSnapshotCache.getIfPresent(RECENT_COMMENT_SNAPSHOT_KEY);
        if (snapshot != null) {
            snapshotCacheHit.increment();
            return snapshot;
        }
        snapshotCacheMiss.increment();
        return recentCommentSnapshotCache.get(RECENT_COMMENT_SNAPSHOT_KEY, ignored -> loadRecentCommentSnapshot());
    }

    private RecentCommentSnapshot loadRecentCommentSnapshot() {
        List<RecentCommentedPostCandidateQueryResult> rows =
                homeQueryRepository.findRecentCommentedPostCandidatesV2(snapshotCandidateSize + 1);
        boolean hasMore = rows.size() > snapshotCandidateSize;
        return new RecentCommentSnapshot(
                rows.stream().limit(snapshotCandidateSize).toList(),
                hasMore
        );
    }

    private Counter cacheCounter(MeterRegistry meterRegistry, String result) {
        return Counter.builder("home.recent.comment.snapshot.cache.requests")
                .description("최근 댓글 후보 스냅숏 캐시 요청")
                .tag("result", result)
                .register(meterRegistry);
    }

    private record RecentCommentSnapshot(
            List<RecentCommentedPostCandidateQueryResult> candidates,
            boolean hasMore
    ) {
        private RecentCommentSnapshot {
            candidates = List.copyOf(candidates);
        }
    }
}
