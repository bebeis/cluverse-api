package cluverse.home.service.implement;

import cluverse.home.properties.HomeRecentCommentProperties;
import cluverse.home.repository.HomeQueryRepository;
import cluverse.home.repository.dto.AccessiblePostQueryResult;
import cluverse.home.repository.dto.RecentCommentedPostCandidateQueryResult;
import cluverse.home.repository.dto.RecentCommentedPostQueryResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HomeReaderTest {

    @Test
    void 최근_댓글_후보가_없으면_빈_목록을_반환한다() {
        // given
        HomeQueryRepository repository = mock(HomeQueryRepository.class);
        HomeReader reader = reader(repository, new SimpleMeterRegistry(), 10);
        when(repository.findRecentCommentedPostCandidatesV2(11)).thenReturn(List.of());

        // when
        List<RecentCommentedPostView> result = reader.readRecentCommentedPostsV2(1L, 10);

        // then
        assertThat(result).isEmpty();
        verify(repository).findRecentCommentedPostCandidatesV2(11);
    }

    @Test
    void 인덱스_집계_후보는_캐시하고_접근_권한은_요청마다_확인한다() {
        // given
        HomeQueryRepository repository = mock(HomeQueryRepository.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        HomeReader reader = reader(repository, meterRegistry, 10);
        LocalDateTime now = LocalDateTime.of(2026, 8, 3, 12, 0);
        when(repository.findRecentCommentedPostCandidatesV2(11)).thenReturn(List.of(
                candidate(3L, now),
                candidate(2L, now.minusSeconds(1)),
                candidate(1L, now.minusSeconds(2))
        ));
        when(repository.findAccessiblePostTitles(1L, List.of(3L, 2L, 1L)))
                .thenReturn(List.of(
                        new AccessiblePostQueryResult(3L, "최근 글"),
                        new AccessiblePostQueryResult(1L, "이전 글")
                ))
                .thenReturn(List.of(new AccessiblePostQueryResult(1L, "이전 글")));

        // when
        List<RecentCommentedPostView> first = reader.readRecentCommentedPostsV2(1L, 10);
        List<RecentCommentedPostView> second = reader.readRecentCommentedPostsV2(1L, 10);

        // then
        assertThat(first).extracting(RecentCommentedPostView::postId)
                .containsExactly(3L, 1L);
        assertThat(second).extracting(RecentCommentedPostView::postId)
                .containsExactly(1L);
        verify(repository).findRecentCommentedPostCandidatesV2(11);
        verify(repository, times(2)).findAccessiblePostTitles(1L, List.of(3L, 2L, 1L));
        assertThat(meterRegistry.counter(
                "home.recent.comment.snapshot.cache.requests", "result", "miss"
        ).count()).isEqualTo(1);
        assertThat(meterRegistry.counter(
                "home.recent.comment.snapshot.cache.requests", "result", "hit"
        ).count()).isEqualTo(1);
    }

    @Test
    void 캐시_후보에서_요청_크기를_채우지_못하면_전체_인덱스_집계로_폴백한다() {
        // given
        HomeQueryRepository repository = mock(HomeQueryRepository.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        HomeReader reader = reader(repository, meterRegistry, 10);
        LocalDateTime now = LocalDateTime.of(2026, 8, 3, 12, 0);
        List<RecentCommentedPostCandidateQueryResult> candidates = LongStream.rangeClosed(1, 11)
                .mapToObj(id -> candidate(id, now.minusSeconds(id)))
                .toList();
        when(repository.findRecentCommentedPostCandidatesV2(11)).thenReturn(candidates);
        when(repository.findAccessiblePostTitles(eq(1L), anyList())).thenReturn(List.of(
                new AccessiblePostQueryResult(1L, "접근 가능 글")
        ));
        when(repository.findRecentCommentedPostsV2Fallback(1L, 2)).thenReturn(List.of(
                new RecentCommentedPostQueryResult(20L, "폴백 최신 글", now),
                new RecentCommentedPostQueryResult(19L, "폴백 이전 글", now.minusSeconds(1))
        ));

        // when
        List<RecentCommentedPostView> result = reader.readRecentCommentedPostsV2(1L, 2);

        // then
        assertThat(result).extracting(RecentCommentedPostView::postId)
                .containsExactly(20L, 19L);
        verify(repository).findRecentCommentedPostsV2Fallback(1L, 2);
        assertThat(meterRegistry.counter("home.recent.comment.snapshot.fallback").count())
                .isEqualTo(1);
    }

    private HomeReader reader(
            HomeQueryRepository repository,
            SimpleMeterRegistry meterRegistry,
            int candidateSize
    ) {
        return new HomeReader(
                repository,
                new HomeRecentCommentProperties(Duration.ofMinutes(1), candidateSize),
                meterRegistry
        );
    }

    private RecentCommentedPostCandidateQueryResult candidate(Long postId, LocalDateTime time) {
        return new RecentCommentedPostCandidateQueryResult(postId, time);
    }
}
