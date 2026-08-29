package cluverse.post.service.implement;

import cluverse.post.domain.PostCategory;
import cluverse.post.properties.PostListCacheProperties;
import cluverse.post.repository.PostListCacheRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostListReaderTest {

    @Test
    void 최신순_앞쪽_페이지는_Redis_ID를_사용하고_상한_카운트_DB_조회도_생략한다() {
        PostReader postReader = mock(PostReader.class);
        PostPageQueryRepository pageRepository = mock(PostPageQueryRepository.class);
        PostListCacheRepository cacheRepository = mock(PostListCacheRepository.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PostListReader reader = reader(postReader, pageRepository, cacheRepository, meterRegistry);
        PostPageSearchRequest request = request(PostSortType.LATEST, 1, 2);
        when(cacheRepository.read(3L, null, 0, 3))
                .thenReturn(Optional.of(new CachedLatestPostIds(List.of(30L, 20L, 10L), 201L)));
        when(postReader.readPostSummaries(7L, List.of(30L, 20L)))
                .thenReturn(List.of(summary(30L), summary(20L)));

        PostPageQueryResult result = reader.readPostPage(7L, request, 201L);

        assertThat(result.posts()).extracting(PostSummaryQueryDto::postId).containsExactly(30L, 20L);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.cappedCount()).isEqualTo(201L);
        verify(postReader, never()).readPostPage(anyLong(), any(PostPageSearchRequest.class));
        verify(pageRepository, never()).findLatestPostCacheEntries(anyLong(), any(), anyInt());
        assertThat(meterRegistry.counter("post.list.cache.requests", "result", "hit").count())
                .isEqualTo(1);
    }

    @Test
    void 캐시_범위를_넘지만_200페이지_이내면_DB_offset_경로로_폴백한다() {
        PostReader postReader = mock(PostReader.class);
        PostPageQueryRepository pageRepository = mock(PostPageQueryRepository.class);
        PostListCacheRepository cacheRepository = mock(PostListCacheRepository.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PostListReader reader = reader(postReader, pageRepository, cacheRepository, meterRegistry);
        PostPageSearchRequest request = request(PostSortType.LATEST, 11, 20);
        PostPageQueryResult databaseResult = new PostPageQueryResult(List.of(summary(10L)), true);
        when(postReader.readPostPage(7L, request)).thenReturn(databaseResult);

        PostPageQueryResult result = reader.readPostPage(7L, request, 401L);

        assertThat(result).isSameAs(databaseResult);
        verify(cacheRepository, never()).read(anyLong(), any(), anyLong(), anyInt());
        assertThat(meterRegistry.counter("post.list.cache.requests", "result", "bypass").count())
                .isEqualTo(1);
    }

    @Test
    void Redis_장애가_나면_DB_offset_조회로_폴백한다() {
        PostReader postReader = mock(PostReader.class);
        PostPageQueryRepository pageRepository = mock(PostPageQueryRepository.class);
        PostListCacheRepository cacheRepository = mock(PostListCacheRepository.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PostListReader reader = reader(postReader, pageRepository, cacheRepository, meterRegistry);
        PostPageSearchRequest request = request(PostSortType.LATEST, 1, 20);
        PostPageQueryResult databaseResult = new PostPageQueryResult(List.of(summary(10L)), false);
        when(cacheRepository.read(3L, null, 0, 21)).thenThrow(new IllegalStateException("redis down"));
        when(postReader.readPostPage(7L, request)).thenReturn(databaseResult);

        PostPageQueryResult result = reader.readPostPage(7L, request, 201L);

        assertThat(result).isSameAs(databaseResult);
        assertThat(meterRegistry.counter("post.list.cache.requests", "result", "error").count())
                .isEqualTo(1);
    }

    private PostListReader reader(
            PostReader postReader,
            PostPageQueryRepository pageRepository,
            PostListCacheRepository cacheRepository,
            SimpleMeterRegistry meterRegistry
    ) {
        return new PostListReader(
                postReader,
                pageRepository,
                cacheRepository,
                new PostListCacheProperties(true, 201, Duration.ofMinutes(3), Duration.ofSeconds(2)),
                meterRegistry
        );
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
