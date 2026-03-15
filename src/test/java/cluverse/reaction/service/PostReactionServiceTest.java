package cluverse.reaction.service;

import cluverse.post.service.PostService;
import cluverse.reaction.service.implement.PostReactionWriter;
import cluverse.reaction.service.response.PostBookmarkResponse;
import cluverse.reaction.service.response.PostLikeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class PostReactionServiceTest {

    @Mock
    private PostReactionWriter postReactionWriter;

    @Mock
    private PostService postService;

    @InjectMocks
    private PostReactionService postReactionService;

    @Test
    void 게시글_좋아요를_추가하고_좋아요_수를_증가시킨다() {
        // when
        PostLikeResponse response = postReactionService.likePost(1L, 10L);

        // then
        InOrder inOrder = inOrder(postReactionWriter, postService);
        inOrder.verify(postReactionWriter).likePost(1L, 10L);
        inOrder.verify(postService).increaseLikeCount(10L);
        verifyNoMoreInteractions(postReactionWriter, postService);
        assertThat(response).isEqualTo(PostLikeResponse.like(10L));
    }

    @Test
    void 게시글_좋아요를_취소하고_좋아요_수를_감소시킨다() {
        // when
        PostLikeResponse response = postReactionService.unlikePost(1L, 10L);

        // then
        InOrder inOrder = inOrder(postReactionWriter, postService);
        inOrder.verify(postReactionWriter).unlikePost(1L, 10L);
        inOrder.verify(postService).decreaseLikeCount(10L);
        verifyNoMoreInteractions(postReactionWriter, postService);
        assertThat(response).isEqualTo(PostLikeResponse.unlike(10L));
    }

    @Test
    void 게시글을_북마크하고_북마크_수를_증가시킨다() {
        // when
        PostBookmarkResponse response = postReactionService.bookmarkPost(1L, 10L);

        // then
        InOrder inOrder = inOrder(postReactionWriter, postService);
        inOrder.verify(postReactionWriter).bookmarkPost(1L, 10L);
        inOrder.verify(postService).increaseBookmarkCount(10L);
        verifyNoMoreInteractions(postReactionWriter, postService);
        assertThat(response).isEqualTo(PostBookmarkResponse.bookmark(10L));
    }

    @Test
    void 게시글_북마크를_취소하고_북마크_수를_감소시킨다() {
        // when
        PostBookmarkResponse response = postReactionService.removeBookmark(1L, 10L);

        // then
        InOrder inOrder = inOrder(postReactionWriter, postService);
        inOrder.verify(postReactionWriter).removeBookmark(1L, 10L);
        inOrder.verify(postService).decreaseBookmarkCount(10L);
        verifyNoMoreInteractions(postReactionWriter, postService);
        assertThat(response).isEqualTo(PostBookmarkResponse.remove(10L));
    }
}
