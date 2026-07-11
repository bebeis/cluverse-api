package cluverse.reaction.service;

import cluverse.feed.repository.dto.FeedPageQueryResult;
import cluverse.feed.repository.dto.FeedPostQueryDto;
import cluverse.board.domain.BoardType;
import cluverse.post.domain.PostCategory;
import cluverse.reaction.service.implement.PostReactionProcessor;
import cluverse.reaction.service.implement.PostReactionReader;
import cluverse.reaction.service.request.BookmarkedPostSearchRequest;
import cluverse.reaction.service.request.BookmarkedPostSortType;
import cluverse.reaction.service.response.BookmarkedPostPageResponse;
import cluverse.reaction.service.response.PostBookmarkResponse;
import cluverse.reaction.service.response.PostLikeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostReactionServiceTest {

    @Mock
    private PostReactionProcessor postReactionProcessor;

    @Mock
    private PostReactionReader postReactionReader;

    @InjectMocks
    private PostReactionService postReactionService;

    @InjectMocks
    private PostReactionQueryService postReactionQueryService;

    @Test
    void 게시글_좋아요는_프로세서에_위임하고_응답을_만든다() {
        // when
        PostLikeResponse response = postReactionService.likePost(1L, 10L);

        // then
        verify(postReactionProcessor).likePost(1L, 10L);
        assertThat(response).isEqualTo(PostLikeResponse.like(10L));
    }

    @Test
    void 게시글_북마크는_프로세서에_위임하고_응답을_만든다() {
        // when
        PostBookmarkResponse response = postReactionService.bookmarkPost(1L, 10L);

        // then
        verify(postReactionProcessor).bookmarkPost(1L, 10L);
        assertThat(response).isEqualTo(PostBookmarkResponse.bookmark(10L));
    }

    @Test
    void 게시글_북마크_취소는_프로세서에_위임하고_응답을_만든다() {
        // when
        PostBookmarkResponse response = postReactionService.removeBookmark(1L, 10L);

        // then
        verify(postReactionProcessor).removeBookmark(1L, 10L);
        assertThat(response).isEqualTo(PostBookmarkResponse.remove(10L));
    }

    @Test
    void 북마크한_게시글_목록을_조회한다() {
        // given
        BookmarkedPostSearchRequest request = new BookmarkedPostSearchRequest(
                BookmarkedPostSortType.BOOKMARKED_AT,
                1,
                20
        );
        when(postReactionReader.readBookmarkedFeed(1L, BookmarkedPostSortType.BOOKMARKED_AT, 1, 20))
                .thenReturn(new FeedPageQueryResult(
                        List.of(new FeedPostQueryDto(
                                10L,
                                3L,
                                BoardType.INTEREST,
                                "AI",
                                null,
                                PostCategory.INFORMATION,
                                "스프링 스터디 모집",
                                "본문 미리보기",
                                List.of("spring"),
                                "https://cdn.example.com/thumb.png",
                                false,
                                false,
                                true,
                                false,
                                false,
                                true,
                                false,
                                12L,
                                4L,
                                1L,
                                2L,
                                2L,
                                "luna",
                                "https://cdn.example.com/profile.png",
                                LocalDateTime.of(2026, 3, 20, 12, 0)
                        )),
                        null,
                        true
                ));

        // when
        BookmarkedPostPageResponse response = postReactionQueryService.getBookmarkedPosts(1L, request);

        // then
        assertThat(response.posts()).hasSize(1);
        assertThat(response.posts().getFirst().postId()).isEqualTo(10L);
        assertThat(response.posts().getFirst().bookmarked()).isTrue();
        assertThat(response.sort()).isEqualTo(BookmarkedPostSortType.BOOKMARKED_AT);
        assertThat(response.hasNext()).isTrue();
    }
}
