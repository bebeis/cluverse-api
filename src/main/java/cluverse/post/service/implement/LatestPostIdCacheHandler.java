package cluverse.post.service.implement;

import cluverse.post.properties.PostListCacheProperties;
import cluverse.post.repository.LatestPostIdCacheRepository;
import cluverse.post.repository.PostPageQueryRepository;
import cluverse.post.repository.dto.CachedLatestPostIds;
import cluverse.post.repository.dto.LatestPostCacheEntry;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import cluverse.post.service.request.PostPageSearchRequest;
import cluverse.post.service.request.PostSortType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Component
@Transactional(readOnly = true)
public class LatestPostIdCacheHandler {

    private final PostReader postReader;
    private final PostPageQueryRepository postPageQueryRepository;
    private final LatestPostIdCacheRepository cacheRepository;
    private final PostListCacheProperties properties;
    private final Counter cacheHit;
    private final Counter cacheMiss;
    private final Counter cacheBypass;
    private final Counter cacheError;

    public LatestPostIdCacheHandler(
            PostReader postReader,
            PostPageQueryRepository postPageQueryRepository,
            LatestPostIdCacheRepository cacheRepository,
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

    public PostPageQueryResult fetch(
            Long memberId,
            PostPageSearchRequest request,
            long countLimit,
            Supplier<PostPageQueryResult> originReader
    ) {
        return fetch(memberId, request, countLimit, originReader, UUID.randomUUID().toString());
    }

    PostPageQueryResult fetch(
            Long memberId,
            PostPageSearchRequest request,
            long countLimit,
            Supplier<PostPageQueryResult> originReader,
            String lockOwner
    ) {
        if (!isCacheable(request)) {
            cacheBypass.increment();
            return originReader.get();
        }

        try {
            // 빈 Optional은 락 경합·버전 충돌·오래된 ID처럼 정상적으로 원본 조회가 필요한 상태다.
            // Redis 명령 자체가 실패한 경우만 CacheOperationException으로 구분해 error 메트릭을 남긴다.
            return readFromCache(memberId, request, countLimit, lockOwner)
                    .orElseGet(originReader);
        } catch (CacheOperationException failure) {
            return fallbackAfterCacheError(request, originReader, failure);
        }
    }

    private Optional<PostPageQueryResult> readFromCache(
            Long memberId,
            PostPageSearchRequest request,
            long countLimit,
            String lockOwner
    ) {
        long offset = offset(request);
        // 마지막 한 건은 응답에 포함하지 않고 다음 페이지 존재 여부를 판단하는 데만 사용한다.
        int fetchSize = request.sizeOrDefault() + 1;
        Optional<CachedLatestPostIds> cached = cacheCall(() -> cacheRepository.read(
                request.boardId(), request.category(), offset, fetchSize));
        if (cached.isPresent()) {
            Optional<PostPageQueryResult> result = project(memberId, request, cached.get(), countLimit);
            result.ifPresent(ignored -> cacheHit.increment());
            return result;
        }

        cacheMiss.increment();
        return warm(memberId, request, countLimit, lockOwner);
    }

    private Optional<PostPageQueryResult> warm(
            Long memberId,
            PostPageSearchRequest request,
            long countLimit,
            String lockOwner
    ) {
        boolean acquired = cacheCall(() -> cacheRepository.tryAcquireWarmupLock(
                request.boardId(), request.category(), lockOwner, properties.warmupLockTtl()));
        if (!acquired) {
            // 락 대기로 요청 지연을 늘리지 않는다. 다른 요청이 워밍하는 동안 이번 요청만 DB로 우회한다.
            return Optional.empty();
        }

        try {
            // 1) 현재 버전을 읽고 2) DB 스냅숏을 만든 뒤 3) 같은 버전일 때만 Redis를 교체한다.
            // 2번 사이에 게시글 쓰기가 커밋되면 무효화가 버전을 올리므로 오래된 스냅숏은 저장되지 않는다.
            long version = cacheCall(() -> cacheRepository.readVersion(
                    request.boardId(), request.category()));
            List<LatestPostCacheEntry> entries = postPageQueryRepository.findLatestPostCacheEntries(
                    request.boardId(), request.category(), properties.maxEntries());
            boolean stored = cacheCall(() -> cacheRepository.replaceIfVersion(
                    request.boardId(), request.category(), version, entries, properties.ttl()));
            if (!stored) {
                return Optional.empty();
            }
            return project(memberId, request, slice(entries, request), countLimit);
        } finally {
            releaseLock(request, lockOwner);
        }
    }

    private Optional<PostPageQueryResult> project(
            Long memberId,
            PostPageSearchRequest request,
            CachedLatestPostIds cached,
            long countLimit
    ) {
        List<Long> pageIds = cached.postIds().stream()
                .limit(request.sizeOrDefault())
                .toList();
        // Redis에는 정렬을 결정하는 ID만 둔다. 제목·반응 수 등 화면 데이터는 선택된 ID만 DB에서 최신값으로 조립한다.
        List<PostSummaryQueryDto> posts = postReader.readPostSummaries(memberId, pageIds);
        if (posts.size() != pageIds.size()) {
            // 쓰기 커밋과 조회 사이에 삭제된 ID가 섞였으면 캐시를 버리고 DB 원본으로 다시 읽는다.
            invalidateSafely(request.boardId());
            return Optional.empty();
        }
        return Optional.of(new PostPageQueryResult(
                posts,
                cached.postIds().size() > request.sizeOrDefault(),
                cappedCount(cached.cachedCount(), countLimit)
        ));
    }

    private CachedLatestPostIds slice(
            List<LatestPostCacheEntry> entries,
            PostPageSearchRequest request
    ) {
        long offset = offset(request);
        int fromIndex = (int) Math.min(offset, entries.size());
        int toIndex = Math.min(fromIndex + request.sizeOrDefault() + 1, entries.size());
        List<Long> fetchedIds = entries.subList(fromIndex, toIndex).stream()
                .map(LatestPostCacheEntry::postId)
                .toList();
        return new CachedLatestPostIds(fetchedIds, entries.size());
    }

    private OptionalLong cappedCount(long cachedCount, long countLimit) {
        if (countLimit > properties.maxEntries()) {
            // Redis의 ZCARD는 캐시한 최신 201개까지만 안다. 더 큰 페이지 블록의 개수는 DB 상한 COUNT로 확인한다.
            return OptionalLong.empty();
        }
        return OptionalLong.of(Math.min(cachedCount, countLimit));
    }

    private boolean isCacheable(PostPageSearchRequest request) {
        long lastRequiredIndex = offset(request) + request.sizeOrDefault();
        // 캐시는 최신순 ID만 보유한다. 다른 정렬이거나 페이지 슬라이스가 201개 범위를 벗어나면 DB가 원천이다.
        return properties.enabled()
                && request.sortOrDefault() == PostSortType.LATEST
                && lastRequiredIndex < properties.maxEntries();
    }

    private long offset(PostPageSearchRequest request) {
        return (long) (request.pageOrDefault() - 1) * request.sizeOrDefault();
    }

    private void releaseLock(PostPageSearchRequest request, String lockOwner) {
        try {
            // unlock Lua가 owner token을 비교하므로 lease 만료 후 다른 요청이 얻은 락은 삭제하지 않는다.
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
            PostPageSearchRequest request,
            Supplier<PostPageQueryResult> originReader,
            CacheOperationException exception
    ) {
        cacheError.increment();
        log.warn("게시글 목록 Redis 캐시 처리 실패. DB 조회로 폴백합니다. boardId={}",
                request.boardId(), exception);
        return originReader.get();
    }

    private <T> T cacheCall(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (RuntimeException exception) {
            // DB 조회 실패까지 캐시 장애로 오인해 재시도하지 않도록 Redis 호출만 경계 안에 둔다.
            throw new CacheOperationException(exception);
        }
    }

    private Counter cacheCounter(MeterRegistry meterRegistry, String result) {
        return Counter.builder("post.list.cache.requests")
                .description("최신순 게시글 ID 캐시 요청")
                .tag("result", result)
                .register(meterRegistry);
    }

    private static final class CacheOperationException extends RuntimeException {

        private CacheOperationException(RuntimeException cause) {
            super(cause);
        }
    }
}
