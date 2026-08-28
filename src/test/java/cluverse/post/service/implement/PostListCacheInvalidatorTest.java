package cluverse.post.service.implement;

import cluverse.post.repository.LatestPostIdCacheRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PostListCacheInvalidatorTest {

    @Test
    void 커밋된_게시글_변경은_해당_게시판의_최신순_캐시를_무효화한다() {
        LatestPostIdCacheRepository repository = mock(LatestPostIdCacheRepository.class);
        PostListCacheInvalidator invalidator = new PostListCacheInvalidator(
                repository, new SimpleMeterRegistry());

        invalidator.invalidate(new PostListChangedEvent(3L));

        verify(repository).invalidateBoard(3L);
    }

    @Test
    void Redis_무효화_실패가_커밋된_게시글_쓰기를_실패로_바꾸지_않는다() {
        LatestPostIdCacheRepository repository = mock(LatestPostIdCacheRepository.class);
        doThrow(new IllegalStateException("redis down")).when(repository).invalidateBoard(3L);
        PostListCacheInvalidator invalidator = new PostListCacheInvalidator(
                repository, new SimpleMeterRegistry());

        assertThatCode(() -> invalidator.invalidate(new PostListChangedEvent(3L)))
                .doesNotThrowAnyException();
    }
}
