package cluverse.reaction.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.reaction.domain.PostBookmark;
import cluverse.reaction.domain.PostLike;
import cluverse.reaction.exception.PostReactionExceptionMessage;
import cluverse.reaction.repository.PostBookmarkRepository;
import cluverse.reaction.repository.PostLikeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostReactionWriterTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostBookmarkRepository postBookmarkRepository;

    @InjectMocks
    private PostReactionWriter postReactionWriter;

    @Test
    void 이미_좋아요한_게시글은_다시_좋아요할_수_없다() {
        // given
        when(postLikeRepository.existsByPostIdAndMemberId(10L, 1L)).thenReturn(true);

        // when, then
        assertThatThrownBy(() -> postReactionWriter.likePost(1L, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(PostReactionExceptionMessage.POST_ALREADY_LIKED.getMessage());
    }

    @Test
    void 좋아요하지_않은_게시글은_좋아요를_취소할_수_없다() {
        // given
        when(postLikeRepository.findByPostIdAndMemberId(10L, 1L)).thenReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> postReactionWriter.unlikePost(1L, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(PostReactionExceptionMessage.POST_NOT_LIKED.getMessage());
    }

    @Test
    void 북마크하지_않은_게시글은_북마크를_취소할_수_없다() {
        // given
        when(postBookmarkRepository.findByMemberIdAndPostId(1L, 10L)).thenReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> postReactionWriter.removeBookmark(1L, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(PostReactionExceptionMessage.POST_NOT_BOOKMARKED.getMessage());
    }

    @Test
    void 게시글을_북마크할_수_있다() {
        // given
        when(postBookmarkRepository.existsByMemberIdAndPostId(1L, 10L)).thenReturn(false);

        // when
        postReactionWriter.bookmarkPost(1L, 10L);

        // then
        verify(postBookmarkRepository).save(argThat(postBookmark ->
                postBookmark.getMemberId().equals(1L) && postBookmark.getPostId().equals(10L)
        ));
    }

    @Test
    void 게시글_좋아요를_취소할_수_있다() {
        // given
        PostLike postLike = PostLike.of(10L, 1L);
        when(postLikeRepository.findByPostIdAndMemberId(10L, 1L)).thenReturn(Optional.of(postLike));

        // when
        postReactionWriter.unlikePost(1L, 10L);

        // then
        verify(postLikeRepository).delete(postLike);
    }
}
