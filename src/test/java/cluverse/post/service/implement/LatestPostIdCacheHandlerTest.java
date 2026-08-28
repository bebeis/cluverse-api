package cluverse.post.service.implement;

import cluverse.post.domain.PostCategory;
import cluverse.post.properties.PostListCacheProperties;
import cluverse.post.repository.LatestPostIdCacheRepository;
import cluverse.post.repository.PostPageQueryRepository;
import cluverse.post.repository.dto.CachedLatestPostIds;
import cluverse.post.repository.dto.LatestPostCacheEntry;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import cluverse.post.service.request.PostPageSearchRequest;
import cluverse.post.service.request.PostSortType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LatestPostIdCacheHandlerTest {

    @Test
    void 최신순_앞쪽_페이지는_Redis_ID를_사용하고_상한_카운트_DB_조회도_생략한다() {
        PostReader postReader = mock(PostReader.class);
        PostPageQueryRepository pageRepository = mock(PostPageQueryRepository.class);
        LatestPostIdCacheRepository cacheRepository = mock(LatestPostIdCacheRepository.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        LatestPostIdCacheHandler handler = handler(postReader, pageRepository, cacheRepository, meterRegistry);
        PostPageSearchRequest request = request(PostSortType.LATEST, 1, 2);
        when(cacheRepository.read(3L, null, 0, 3))
                .thenReturn(Optional.of(new CachedLatestPostIds(List.of(30L, 20L, 10L), 201L)));
        when(postReader.readPostSummaries(7L, List.of(30L, 20L)))
                .thenReturn(List.of(summary(30L), summary(20L)));

        PostPageQueryResult result = handler.fetch(7L, request, 201L, unusedOrigin());

        assertThat(result.posts()).extracting(PostSummaryQueryDto::postId).containsExactly(30L, 20L);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.cappedCount()).hasValue(201L);
        verify(pageRepository, never()).findLatestPostCacheEntries(anyLong(), any(), anyInt());
        assertThat(meterRegistry.counter("post.list.cache.requests", "result", "hit").count())
                .isEqualTo(1);
    }

    @Test
    void 캐시_범위를_넘지만_200페이지_이내면_DB_offset_경로로_폴백한다() {
        PostReader postReader = mock(PostReader.class);
        PostPageQueryRepository pageRepository = mock(PostPageQueryRepository.class);
        LatestPostIdCacheRepository cacheRepository = mock(LatestPostIdCacheRepository.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        LatestPostIdCacheHandler handler = handler(postReader, pageRepository, cacheRepository, meterRegistry);
        PostPageSearchRequest request = request(PostSortType.LATEST, 11, 20);
        PostPageQueryResult databaseResult = new PostPageQueryResult(List.of(summary(10L)), true);

        PostPageQueryResult result = handler.fetch(7L, request, 401L, origin(databaseResult));

        assertThat(result).isSameAs(databaseResult);
        verify(cacheRepository, never()).read(anyLong(), any(), anyLong(), anyInt());
        assertThat(meterRegistry.counter("post.list.cache.requests", "result", "bypass").count())
                .isEqualTo(1);
    }

    @Test
    void Redis_장애가_나면_DB_offset_조회로_폴백한다() {
        PostReader postReader = mock(PostReader.class);
        PostPageQueryRepository pageRepository = mock(PostPageQueryRepository.class);
        LatestPostIdCacheRepository cacheRepository = mock(LatestPostIdCacheRepository.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        LatestPostIdCacheHandler handler = handler(postReader, pageRepository, cacheRepository, meterRegistry);
        PostPageSearchRequest request = request(PostSortType.LATEST, 1, 20);
        PostPageQueryResult databaseResult = new PostPageQueryResult(List.of(summary(10L)), false);
        when(cacheRepository.read(3L, null, 0, 21)).thenThrow(new IllegalStateException("redis down"));

        PostPageQueryResult result = handler.fetch(7L, request, 201L, origin(databaseResult));

        assertThat(result).isSameAs(databaseResult);
        assertThat(meterRegistry.counter("post.list.cache.requests", "result", "error").count())
                .isEqualTo(1);
    }

    @Test
    void 캐시_ID의_DB_프로젝션_실패는_Redis_장애로_숨기지_않는다() {
        PostReader postReader = mock(PostReader.class);
        PostPageQueryRepository pageRepository = mock(PostPageQueryRepository.class);
        LatestPostIdCacheRepository cacheRepository = mock(LatestPostIdCacheRepository.class);
        LatestPostIdCacheHandler handler = handler(
                postReader, pageRepository, cacheRepository, new SimpleMeterRegistry());
        PostPageSearchRequest request = request(PostSortType.LATEST, 1, 2);
        when(cacheRepository.read(3L, null, 0, 3))
                .thenReturn(Optional.of(new CachedLatestPostIds(List.of(30L, 20L, 10L), 201L)));
        when(postReader.readPostSummaries(7L, List.of(30L, 20L)))
                .thenThrow(new IllegalStateException("database down"));

        assertThatThrownBy(() -> handler.fetch(7L, request, 201L, unusedOrigin()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database down");

    }

    @Test
    void 캐시_miss에서_락을_얻은_요청이_버전을_확인하고_최신_ID를_적재한다() {
        PostReader postReader = mock(PostReader.class);
        PostPageQueryRepository pageRepository = mock(PostPageQueryRepository.class);
        LatestPostIdCacheRepository cacheRepository = mock(LatestPostIdCacheRepository.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        LatestPostIdCacheHandler handler = handler(postReader, pageRepository, cacheRepository, meterRegistry);
        PostPageSearchRequest request = request(PostSortType.LATEST, 1, 2);
        List<LatestPostCacheEntry> entries = List.of(
                new LatestPostCacheEntry(30L, LocalDateTime.of(2026, 8, 6, 12, 0)),
                new LatestPostCacheEntry(20L, LocalDateTime.of(2026, 8, 6, 11, 0)),
                new LatestPostCacheEntry(10L, LocalDateTime.of(2026, 8, 6, 10, 0))
        );
        when(cacheRepository.read(3L, null, 0, 3)).thenReturn(Optional.empty());
        when(cacheRepository.tryAcquireWarmupLock(anyLong(), any(), any(), any()))
                .thenReturn(true);
        when(cacheRepository.readVersion(3L, null)).thenReturn(7L);
        when(pageRepository.findLatestPostCacheEntries(3L, null, 201)).thenReturn(entries);
        when(cacheRepository.replaceIfVersion(3L, null, 7L, entries, Duration.ofMinutes(3)))
                .thenReturn(true);
        when(postReader.readPostSummaries(7L, List.of(30L, 20L)))
                .thenReturn(List.of(summary(30L), summary(20L)));

        PostPageQueryResult result = handler.fetch(7L, request, 201L, unusedOrigin(), "owner");

        assertThat(result.posts()).extracting(PostSummaryQueryDto::postId).containsExactly(30L, 20L);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.cappedCount()).hasValue(3L);
        verify(cacheRepository).releaseWarmupLock(3L, null, "owner");
    }

    @Test
    void 캐시_워밍_락을_얻지_못한_요청은_DB로_폴백한다() {
        PostReader postReader = mock(PostReader.class);
        PostPageQueryRepository pageRepository = mock(PostPageQueryRepository.class);
        LatestPostIdCacheRepository cacheRepository = mock(LatestPostIdCacheRepository.class);
        LatestPostIdCacheHandler handler = handler(
                postReader, pageRepository, cacheRepository, new SimpleMeterRegistry());
        PostPageSearchRequest request = request(PostSortType.LATEST, 1, 20);
        PostPageQueryResult databaseResult = new PostPageQueryResult(List.of(summary(10L)), false);
        when(cacheRepository.read(3L, null, 0, 21)).thenReturn(Optional.empty());
        when(cacheRepository.tryAcquireWarmupLock(anyLong(), any(), any(), any()))
                .thenReturn(false);

        PostPageQueryResult result = handler.fetch(
                7L, request, 201L, origin(databaseResult), "owner");

        assertThat(result).isSameAs(databaseResult);
        verify(pageRepository, never()).findLatestPostCacheEntries(anyLong(), any(), anyInt());
    }

    @Test
    void 워밍_중_버전이_바뀌면_오래된_ID를_저장하지_않고_DB로_폴백한다() {
        PostReader postReader = mock(PostReader.class);
        PostPageQueryRepository pageRepository = mock(PostPageQueryRepository.class);
        LatestPostIdCacheRepository cacheRepository = mock(LatestPostIdCacheRepository.class);
        LatestPostIdCacheHandler handler = handler(
                postReader, pageRepository, cacheRepository, new SimpleMeterRegistry());
        PostPageSearchRequest request = request(PostSortType.LATEST, 1, 20);
        PostPageQueryResult databaseResult = new PostPageQueryResult(List.of(summary(10L)), false);
        List<LatestPostCacheEntry> entries = List.of(
                new LatestPostCacheEntry(10L, LocalDateTime.of(2026, 8, 6, 12, 0)));
        when(cacheRepository.read(3L, null, 0, 21)).thenReturn(Optional.empty());
        when(cacheRepository.tryAcquireWarmupLock(anyLong(), any(), any(), any())).thenReturn(true);
        when(cacheRepository.readVersion(3L, null)).thenReturn(7L);
        when(pageRepository.findLatestPostCacheEntries(3L, null, 201)).thenReturn(entries);
        when(cacheRepository.replaceIfVersion(3L, null, 7L, entries, Duration.ofMinutes(3)))
                .thenReturn(false);

        PostPageQueryResult result = handler.fetch(
                7L, request, 201L, origin(databaseResult), "owner");

        assertThat(result).isSameAs(databaseResult);
        verify(cacheRepository).releaseWarmupLock(3L, null, "owner");
    }

    @Test
    void 캐시_ID가_DB와_다르면_캐시를_무효화하고_DB로_다시_조회한다() {
        PostReader postReader = mock(PostReader.class);
        PostPageQueryRepository pageRepository = mock(PostPageQueryRepository.class);
        LatestPostIdCacheRepository cacheRepository = mock(LatestPostIdCacheRepository.class);
        LatestPostIdCacheHandler handler = handler(
                postReader, pageRepository, cacheRepository, new SimpleMeterRegistry());
        PostPageSearchRequest request = request(PostSortType.LATEST, 1, 2);
        PostPageQueryResult databaseResult = new PostPageQueryResult(List.of(summary(30L)), false);
        when(cacheRepository.read(3L, null, 0, 3))
                .thenReturn(Optional.of(new CachedLatestPostIds(List.of(30L, 20L, 10L), 201L)));
        when(postReader.readPostSummaries(7L, List.of(30L, 20L)))
                .thenReturn(List.of(summary(30L)));

        PostPageQueryResult result = handler.fetch(
                7L, request, 201L, origin(databaseResult), "owner");

        assertThat(result).isSameAs(databaseResult);
        verify(cacheRepository).invalidateBoard(3L);
    }

    @Test
    void 캐시가_보유한_범위보다_큰_카운트가_필요하면_DB_COUNT를_요청하도록_빈_값을_반환한다() {
        PostReader postReader = mock(PostReader.class);
        PostPageQueryRepository pageRepository = mock(PostPageQueryRepository.class);
        LatestPostIdCacheRepository cacheRepository = mock(LatestPostIdCacheRepository.class);
        LatestPostIdCacheHandler handler = handler(
                postReader, pageRepository, cacheRepository, new SimpleMeterRegistry());
        PostPageSearchRequest request = request(PostSortType.LATEST, 1, 2);
        when(cacheRepository.read(3L, null, 0, 3))
                .thenReturn(Optional.of(new CachedLatestPostIds(List.of(30L, 20L, 10L), 201L)));
        when(postReader.readPostSummaries(7L, List.of(30L, 20L)))
                .thenReturn(List.of(summary(30L), summary(20L)));

        PostPageQueryResult result = handler.fetch(7L, request, 401L, unusedOrigin(), "owner");

        assertThat(result.cappedCount()).isEmpty();
    }

    private LatestPostIdCacheHandler handler(
            PostReader postReader,
            PostPageQueryRepository pageRepository,
            LatestPostIdCacheRepository cacheRepository,
            SimpleMeterRegistry meterRegistry
    ) {
        return new LatestPostIdCacheHandler(
                postReader,
                pageRepository,
                cacheRepository,
                new PostListCacheProperties(true, 201, Duration.ofMinutes(3), Duration.ofSeconds(2)),
                meterRegistry
        );
    }

    private Supplier<PostPageQueryResult> origin(PostPageQueryResult result) {
        return () -> result;
    }

    private Supplier<PostPageQueryResult> unusedOrigin() {
        return () -> {
            throw new AssertionError("원본 DB 조회가 실행되면 안 됩니다.");
        };
    }

    private PostPageSearchRequest request(PostSortType sort, int page, int size) {
        return new PostPageSearchRequest(3L, null, sort, page, size);
    }

    private PostSummaryQueryDto summary(Long postId) {
        return new PostSummaryQueryDto(
                postId,
                3L,
                PostCategory.INFORMATION,
                "제목",
                "미리보기",
                List.of(),
                null,
                false,
                false,
                true,
                false,
                0L,
                0L,
                0L,
                0L,
                1L,
                "작성자",
                null,
                LocalDateTime.of(2026, 8, 6, 12, 0)
        );
    }
}
