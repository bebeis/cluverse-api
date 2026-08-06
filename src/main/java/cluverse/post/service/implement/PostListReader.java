package cluverse.post.service.implement;

import cluverse.post.properties.PostListCacheProperties;
import cluverse.post.repository.PostListCacheRepository;
import cluverse.post.repository.PostPageQueryRepository;
import cluverse.post.repository.dto.CachedLatestPostIds;
import cluverse.post.repository.dto.LatestPostCacheEntry;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import cluverse.post.service.request.PostSearchRequest;
import cluverse.post.service.request.PostSortType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@Transactional(readOnly = true)
public class PostListReader {

    private final PostReader postReader;
    private final PostPageQueryRepository postPageQueryRepository;
    private final PostListCacheRepository cacheRepository;
    private final PostListCacheProperties properties;
    private final Counter cacheHit;
    private final Counter cacheMiss;
    private final Counter cacheBypass;
    private final Counter cacheError;

    public PostListReader(
            PostReader postReader,
            PostPageQueryRepository postPageQueryRepository,
            PostListCacheRepository cacheRepository,
            PostListCacheProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.postReader = postReader;
        this.postPageQueryRepository = postPageQueryRepository;
        this.cacheRepository = cacheRepository;
        this.properties = properties;
        this.cacheHit = cacheCounter(meterRegistry, "hit");
        this.cacheMiss = cacheCounter(meterRegistry, "miss");
        this.cacheBypass = cacheCounter(meterRegistry, "bypass");
        this.cacheError = cacheCounter(meterRegistry, "error");
    }

    public PostPageQueryResult readPostPage(
            Long memberId,
            PostSearchRequest request,
            long countLimit
    ) {
        return readPostPage(memberId, request, countLimit, UUID.randomUUID().toString());
    }

    PostPageQueryResult readPostPage(
            Long memberId,
            PostSearchRequest request,
            long countLimit,
            String lockOwner
    ) {
        if (!isCacheable(request)) {
            cacheBypass.increment();
            return postReader.readPostPage(memberId, request);
        }

        long offset = offset(request);
        int fetchSize = request.sizeOrDefault() + 1;
        Optional<CachedLatestPostIds> cached;
        try {
            cached = cacheRepository.read(request.boardId(), request.category(), offset, fetchSize);
        } catch (RuntimeException exception) {
            return fallbackAfterCacheError(memberId, request, exception);
        }
        if (cached.isPresent()) {
            PostPageQueryResult result = projectCachedIds(memberId, request, cached.get(), countLimit);
            if (result != null) {
                cacheHit.increment();
                return result;
            }
            invalidateSafely(request.boardId());
            return postReader.readPostPage(memberId, request);
        }

        cacheMiss.increment();
        return warmOrFallback(memberId, request, countLimit, lockOwner);
    }

    private PostPageQueryResult warmOrFallback(
            Long memberId,
            PostSearchRequest request,
            long countLimit,
            String lockOwner
    ) {
        boolean acquired;
        try {
            acquired = cacheRepository.tryAcquireWarmupLock(
                    request.boardId(), request.category(), lockOwner, properties.warmupLockTtl());
        } catch (RuntimeException exception) {
            return fallbackAfterCacheError(memberId, request, exception);
        }
        if (!acquired) {
            return postReader.readPostPage(memberId, request);
        }

        try {
            long version;
            try {
                version = cacheRepository.readVersion(request.boardId(), request.category());
            } catch (RuntimeException exception) {
                return fallbackAfterCacheError(memberId, request, exception);
            }
            List<LatestPostCacheEntry> entries = postPageQueryRepository.findLatestPostCacheEntries(
                    request.boardId(), request.category(), properties.maxEntries());
            boolean stored;
            try {
                stored = cacheRepository.replaceIfVersion(
                        request.boardId(), request.category(), version, entries, properties.ttl());
            } catch (RuntimeException exception) {
                return fallbackAfterCacheError(memberId, request, exception);
            }
            if (!stored) {
                return postReader.readPostPage(memberId, request);
            }
            return projectEntries(memberId, request, entries, countLimit);
        } finally {
            releaseLock(request, lockOwner);
        }
    }

    private PostPageQueryResult projectCachedIds(
            Long memberId,
            PostSearchRequest request,
            CachedLatestPostIds cached,
            long countLimit
    ) {
        List<Long> pageIds = cached.postIds().stream()
                .limit(request.sizeOrDefault())
                .toList();
        List<PostSummaryQueryDto> posts = postReader.readPostSummaries(memberId, pageIds);
        if (posts.size() != pageIds.size()) {
            return null;
        }
        return new PostPageQueryResult(
                posts,
                cached.postIds().size() > request.sizeOrDefault(),
                cappedCount(cached.cachedCount(), countLimit)
        );
    }

    private PostPageQueryResult projectEntries(
            Long memberId,
            PostSearchRequest request,
            List<LatestPostCacheEntry> entries,
            long countLimit
    ) {
        long offset = offset(request);
        int fromIndex = (int) Math.min(offset, entries.size());
        int toIndex = Math.min(fromIndex + request.sizeOrDefault() + 1, entries.size());
        List<Long> fetchedIds = entries.subList(fromIndex, toIndex).stream()
                .map(LatestPostCacheEntry::postId)
                .toList();
        List<Long> pageIds = fetchedIds.stream().limit(request.sizeOrDefault()).toList();
        List<PostSummaryQueryDto> posts = postReader.readPostSummaries(memberId, pageIds);
        if (posts.size() != pageIds.size()) {
            invalidateSafely(request.boardId());
            return postReader.readPostPage(memberId, request);
        }
        return new PostPageQueryResult(
                posts,
                fetchedIds.size() > request.sizeOrDefault(),
                cappedCount(entries.size(), countLimit)
        );
    }

    private Long cappedCount(long cachedCount, long countLimit) {
        if (countLimit > properties.maxEntries()) {
            return null;
        }
        return Math.min(cachedCount, countLimit);
    }

    private boolean isCacheable(PostSearchRequest request) {
        long lastRequiredIndex = offset(request) + request.sizeOrDefault();
        return properties.enabled()
                && !request.isDateBased()
                && request.sortOrDefault() == PostSortType.LATEST
                && lastRequiredIndex < properties.maxEntries();
    }

    private long offset(PostSearchRequest request) {
        return (long) (request.pageOrDefault() - 1) * request.sizeOrDefault();
    }

    private void releaseLock(PostSearchRequest request, String lockOwner) {
        try {
            cacheRepository.releaseWarmupLock(request.boardId(), request.category(), lockOwner);
        } catch (RuntimeException exception) {
            log.warn("게시글 목록 캐시 워밍 락 해제 실패. lease 만료를 기다립니다. boardId={}",
                    request.boardId(), exception);
        }
    }

    private void invalidateSafely(Long boardId) {
        try {
            cacheRepository.invalidateBoard(boardId);
        } catch (RuntimeException exception) {
            cacheError.increment();
            log.warn("오래된 게시글 목록 캐시 무효화 실패. TTL 만료로 복구합니다. boardId={}",
                    boardId, exception);
        }
    }

    private PostPageQueryResult fallbackAfterCacheError(
            Long memberId,
            PostSearchRequest request,
            RuntimeException exception
    ) {
        cacheError.increment();
        log.warn("게시글 목록 Redis 캐시 처리 실패. DB 조회로 폴백합니다. boardId={}",
                request.boardId(), exception);
        return postReader.readPostPage(memberId, request);
    }

    private Counter cacheCounter(MeterRegistry meterRegistry, String result) {
        return Counter.builder("post.list.cache.requests")
                .description("최신순 게시글 ID 캐시 요청")
                .tag("result", result)
                .register(meterRegistry);
    }
}
