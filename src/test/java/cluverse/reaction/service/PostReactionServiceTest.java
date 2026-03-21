package cluverse.reaction.service;

import cluverse.feed.repository.FeedQueryRepository;
import cluverse.feed.repository.dto.FeedPageQueryResult;
import cluverse.feed.repository.dto.FeedPostQueryDto;
import cluverse.board.domain.BoardType;
import cluverse.post.domain.PostCategory;
import cluverse.meta.service.PostMetaService;
import cluverse.post.service.PostAccessService;
import cluverse.reaction.service.implement.PostReactionWriter;
import cluverse.reaction.service.request.BookmarkedPostSearchRequest;
import cluverse.reaction.service.request.BookmarkedPostSortType;
import cluverse.reaction.service.response.BookmarkedPostPageResponse;
import cluverse.reaction.service.response.PostBookmarkResponse;
import cluverse.reaction.service.response.PostLikeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class PostReactionServiceTest {

    @Mock
    private PostReactionWriter postReactionWriter;

    @Mock
    private PostAccessService postAccessService;

    @Mock
    private PostMetaService postMetaService;

    @Mock
    private FeedQueryRepository feedQueryRepository;

    @InjectMocks
    private PostReactionService postReactionService;

    @Test
    void 게시글_좋아요를_추가하고_좋아요_수를_증가시킨다() {
        // when
        PostLikeResponse response = postReactionService.likePost(1L, 10L);

        // then
        InOrder inOrder = inOrder(postAccessService, postReactionWriter, postMetaService);
        inOrder.verify(postAccessService).validateReadablePost(1L, 10L);
        inOrder.verify(postReactionWriter).likePost(1L, 10L);
        inOrder.verify(postMetaService).increaseLikeCount(10L);
        verifyNoMoreInteractions(postAccessService, postReactionWriter, postMetaService);
        assertThat(response).isEqualTo(PostLikeResponse.like(10L));
    }

    @Test
    void 게시글을_북마크하고_북마크_수를_증가시킨다() {
        // when
        PostBookmarkResponse response = postReactionService.bookmarkPost(1L, 10L);

        // then
        InOrder inOrder = inOrder(postAccessService, postReactionWriter, postMetaService);
        inOrder.verify(postAccessService).validateReadablePost(1L, 10L);
        inOrder.verify(postReactionWriter).bookmarkPost(1L, 10L);
        inOrder.verify(postMetaService).increaseBookmarkCount(10L);
        verifyNoMoreInteractions(postAccessService, postReactionWriter, postMetaService);
        assertThat(response).isEqualTo(PostBookmarkResponse.bookmark(10L));
    }

    @Test
    void 게시글_북마크를_취소하고_북마크_수를_감소시킨다() {
        // when
        PostBookmarkResponse response = postReactionService.removeBookmark(1L, 10L);

        // then
        InOrder inOrder = inOrder(postAccessService, postReactionWriter, postMetaService);
        inOrder.verify(postAccessService).validateReadablePost(1L, 10L);
        inOrder.verify(postReactionWriter).removeBookmark(1L, 10L);
        inOrder.verify(postMetaService).decreaseBookmarkCount(10L);
        verifyNoMoreInteractions(postAccessService, postReactionWriter, postMetaService);
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
        when(feedQueryRepository.findBlockedMemberIds(1L)).thenReturn(Set.of(99L));
        when(feedQueryRepository.findMyGroupBoardIds(1L)).thenReturn(Set.of(30L));
        when(feedQueryRepository.findBookmarkedFeedPage(1L, Set.of(99L), Set.of(30L), BookmarkedPostSortType.BOOKMARKED_AT, 1, 20))
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
        BookmarkedPostPageResponse response = postReactionService.getBookmarkedPosts(1L, request);

        // then
        assertThat(response.posts()).hasSize(1);
        assertThat(response.posts().getFirst().postId()).isEqualTo(10L);
        assertThat(response.posts().getFirst().bookmarked()).isTrue();
        assertThat(response.sort()).isEqualTo(BookmarkedPostSortType.BOOKMARKED_AT);
        assertThat(response.hasNext()).isTrue();
    }
}
