package cluverse.post.service.implement;

import cluverse.post.domain.PostCategory;
import cluverse.post.properties.PostListCacheProperties;
import cluverse.post.repository.LatestPostIdCacheRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LatestPostIdCacheWriterTest {

    @Test
    void 생성된_게시글을_전체와_카테고리의_준비된_캐시에_추가한다() {
        LatestPostIdCacheRepository repository = mock(LatestPostIdCacheRepository.class);
        PostListCacheProperties properties = properties();
        LatestPostIdCacheWriter writer = new LatestPostIdCacheWriter(
                repository, properties, new SimpleMeterRegistry());
        PostCreatedEvent event = event();
        when(repository.addIfReady(
                3L, null, 10L, event.createdAt(), properties.maxEntries(), properties.ttl()))
                .thenReturn(true);
        when(repository.addIfReady(
                3L, PostCategory.INFORMATION, 10L, event.createdAt(),
                properties.maxEntries(), properties.ttl()))
                .thenReturn(true);

        writer.add(event);

        verify(repository).addIfReady(
                3L, null, 10L, event.createdAt(), properties.maxEntries(), properties.ttl());
        verify(repository).addIfReady(
                3L, PostCategory.INFORMATION, 10L, event.createdAt(),
                properties.maxEntries(), properties.ttl());
    }

    @Test
    void 일부_WriteThrough이_실패하면_게시판_캐시를_무효화한다() {
        LatestPostIdCacheRepository repository = mock(LatestPostIdCacheRepository.class);
        PostListCacheProperties properties = properties();
        LatestPostIdCacheWriter writer = new LatestPostIdCacheWriter(
                repository, properties, new SimpleMeterRegistry());
        PostCreatedEvent event = event();
        doThrow(new IllegalStateException("redis down")).when(repository).addIfReady(
                3L, PostCategory.INFORMATION, 10L, event.createdAt(),
                properties.maxEntries(), properties.ttl());

        assertThatCode(() -> writer.add(event)).doesNotThrowAnyException();

        verify(repository).invalidateBoard(3L);
    }

    @Test
    void 아직_워밍되지_않은_캐시는_새로_만들지_않고_다음_조회에_맡긴다() {
        LatestPostIdCacheRepository repository = mock(LatestPostIdCacheRepository.class);
        PostListCacheProperties properties = properties();
        LatestPostIdCacheWriter writer = new LatestPostIdCacheWriter(
                repository, properties, new SimpleMeterRegistry());

        writer.add(event());

        verify(repository, never()).invalidateBoard(3L);
    }

    @Test
    void Redis_장애가_계속되어_무효화도_실패해도_게시글_생성을_실패로_바꾸지_않는다() {
        LatestPostIdCacheRepository repository = mock(LatestPostIdCacheRepository.class);
        PostListCacheProperties properties = properties();
        LatestPostIdCacheWriter writer = new LatestPostIdCacheWriter(
                repository, properties, new SimpleMeterRegistry());
        PostCreatedEvent event = event();
        doThrow(new IllegalStateException("redis down")).when(repository).addIfReady(
                3L, null, 10L, event.createdAt(), properties.maxEntries(), properties.ttl());
        doThrow(new IllegalStateException("redis still down")).when(repository).invalidateBoard(3L);

        assertThatCode(() -> writer.add(event)).doesNotThrowAnyException();
    }

    private PostCreatedEvent event() {
        return new PostCreatedEvent(
                3L,
                10L,
                PostCategory.INFORMATION,
                LocalDateTime.of(2026, 8, 28, 13, 0)
        );
    }

    private PostListCacheProperties properties() {
        return new PostListCacheProperties(
                true,
                201,
                Duration.ofMinutes(3),
                Duration.ofSeconds(2)
        );
    }
}
