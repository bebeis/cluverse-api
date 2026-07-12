package cluverse.post.service;

import cluverse.board.service.implement.BoardReader;
import cluverse.post.domain.PostCategory;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import cluverse.post.service.implement.PostReader;
import cluverse.post.service.request.PostCursorDirection;
import cluverse.post.service.request.PostCursorSearchRequest;
import cluverse.post.service.response.PostCursorPageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostListQueryServiceV4Test {

    @Mock
    private PostReader postReader;

    @Mock
    private BoardReader boardReader;

    @InjectMocks
    private PostListQueryServiceV4 postListQueryServiceV4;

    @Test
    void 커서는_첫_게시글과_마지막_게시글의_시각과_ID로_만들어진다() {
        // given
        PostCursorSearchRequest request = new PostCursorSearchRequest(3L, null, 2, null, null, null, null);
        LocalDateTime first = LocalDateTime.of(2026, 1, 20, 12, 0);
        LocalDateTime last = LocalDateTime.of(2026, 1, 19, 9, 0);
        when(postReader.readPostPageByCursor(99L, request)).thenReturn(new PostPageQueryResult(
                List.of(createSummary(10L, first), createSummary(7L, last)),
                true
        ));

        // when
        PostCursorPageResponse response = postListQueryServiceV4.getPosts(99L, request);

        // then
        assertThat(response.prevCursor().createdAt()).isEqualTo(first);
        assertThat(response.prevCursor().postId()).isEqualTo(10L);
        assertThat(response.nextCursor().createdAt()).isEqualTo(last);
        assertThat(response.nextCursor().postId()).isEqualTo(7L);
        verify(boardReader).validateReadable(99L, 3L);
    }

    @Test
    void 무앵커_진입은_hasPrev가_false다() {
        // given
        PostCursorSearchRequest request = new PostCursorSearchRequest(3L, null, 20, null, null, null, null);
        when(postReader.readPostPageByCursor(99L, request))
                .thenReturn(new PostPageQueryResult(List.of(), true));

        // when
        PostCursorPageResponse response = postListQueryServiceV4.getPosts(99L, request);

        // then
        assertThat(response.hasPrev()).isFalse();
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void 날짜_진입은_앵커보다_최신_글_존재_여부로_hasPrev를_판단한다() {
        // given
        PostCursorSearchRequest request = new PostCursorSearchRequest(
                3L, PostCategory.INFORMATION, 20, LocalDate.of(2026, 1, 20), null, null, null);
        when(postReader.readPostPageByCursor(99L, request))
                .thenReturn(new PostPageQueryResult(List.of(), false));
        when(postReader.existsPostsNewerThan(request)).thenReturn(true);

        // when
        PostCursorPageResponse response = postListQueryServiceV4.getPosts(99L, request);

        // then
        assertThat(response.hasPrev()).isTrue();
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void NEXT_이동은_hasPrev가_항상_true다() {
        // given: 커서가 있다는 것 자체가 이전 페이지의 존재를 뜻한다
        PostCursorSearchRequest request = new PostCursorSearchRequest(
                3L, null, 20, null, LocalDateTime.of(2026, 1, 20, 12, 0), 100L, PostCursorDirection.NEXT);
        when(postReader.readPostPageByCursor(99L, request))
                .thenReturn(new PostPageQueryResult(List.of(), false));

        // when
        PostCursorPageResponse response = postListQueryServiceV4.getPosts(99L, request);

        // then
        assertThat(response.hasPrev()).isTrue();
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void PREV_이동은_슬라이스로_hasPrev를_hasNext는_항상_true로_판단한다() {
        // given
        PostCursorSearchRequest request = new PostCursorSearchRequest(
                3L, null, 20, null, LocalDateTime.of(2026, 1, 20, 12, 0), 100L, PostCursorDirection.PREV);
        when(postReader.readPostPageByCursor(99L, request))
                .thenReturn(new PostPageQueryResult(List.of(), true));

        // when
        PostCursorPageResponse response = postListQueryServiceV4.getPosts(99L, request);

        // then
        assertThat(response.hasPrev()).isTrue();
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void 결과가_비면_커서도_null이다() {
        // given
        PostCursorSearchRequest request = new PostCursorSearchRequest(3L, null, 20, null, null, null, null);
        when(postReader.readPostPageByCursor(99L, request))
                .thenReturn(new PostPageQueryResult(List.of(), false));

        // when
        PostCursorPageResponse response = postListQueryServiceV4.getPosts(99L, request);

        // then
        assertThat(response.posts()).isEmpty();
        assertThat(response.prevCursor()).isNull();
        assertThat(response.nextCursor()).isNull();
    }

    private PostSummaryQueryDto createSummary(Long postId, LocalDateTime createdAt) {
        return new PostSummaryQueryDto(
                postId, 3L, PostCategory.INFORMATION, "제목", "미리보기", List.of(), null,
                false, false, true, false,
                0L, 0L, 0L, 0L,
                2L, "luna", null, createdAt
        );
    }
}
