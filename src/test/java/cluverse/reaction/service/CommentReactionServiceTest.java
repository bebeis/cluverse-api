package cluverse.reaction.service;

import cluverse.comment.service.response.CommentReactionTargetResponse;
import cluverse.reaction.service.implement.CommentReactionProcessor;
import cluverse.reaction.service.response.CommentLikeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentReactionServiceTest {

    @Mock
    private CommentReactionProcessor commentReactionProcessor;

    @InjectMocks
    private CommentReactionService commentReactionService;

    @Test
    void 댓글_좋아요시_프로세서_결과로_응답을_만든다() {
        // given
        when(commentReactionProcessor.likeComment(1L, 101L))
                .thenReturn(new CommentReactionTargetResponse(10L, 101L));

        // when
        CommentLikeResponse response = commentReactionService.likeComment(1L, 101L);

        // then
        assertThat(response.postId()).isEqualTo(10L);
        assertThat(response.commentId()).isEqualTo(101L);
        assertThat(response.liked()).isTrue();
        verify(commentReactionProcessor).likeComment(1L, 101L);
    }

    @Test
    void 댓글_좋아요_취소시_프로세서_결과로_응답을_만든다() {
        // given
        when(commentReactionProcessor.unlikeComment(1L, 101L))
                .thenReturn(new CommentReactionTargetResponse(10L, 101L));

        // when
        CommentLikeResponse response = commentReactionService.unlikeComment(1L, 101L);

        // then
        assertThat(response.postId()).isEqualTo(10L);
        assertThat(response.commentId()).isEqualTo(101L);
        assertThat(response.liked()).isFalse();
        verify(commentReactionProcessor).unlikeComment(1L, 101L);
    }
}
