package cluverse.reaction.service.implement;

import cluverse.comment.service.implement.CommentReader;
import cluverse.comment.service.implement.CommentWriter;
import cluverse.comment.service.response.CommentReactionTargetResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentReactionProcessorTest {

    @Mock
    private CommentReactionWriter commentReactionWriter;

    @Mock
    private CommentReader commentReader;

    @Mock
    private CommentWriter commentWriter;

    @InjectMocks
    private CommentReactionProcessor commentReactionProcessor;

    @Test
    void 댓글_좋아요시_댓글_검증후_좋아요_수까지_증가시킨다() {
        // given
        when(commentReader.readReactionTarget(101L)).thenReturn(new CommentReactionTargetResponse(10L, 101L));

        // when
        CommentReactionTargetResponse target = commentReactionProcessor.likeComment(1L, 101L);

        // then
        assertThat(target.postId()).isEqualTo(10L);
        assertThat(target.commentId()).isEqualTo(101L);
        verify(commentReader).readReactionTarget(101L);
        verify(commentReactionWriter).likeComment(1L, 101L);
        verify(commentWriter).increaseLikeCount(101L);
    }

    @Test
    void 댓글_좋아요_취소시_좋아요_수까지_감소시킨다() {
        // given
        when(commentReader.readReactionTarget(101L)).thenReturn(new CommentReactionTargetResponse(10L, 101L));

        // when
        CommentReactionTargetResponse target = commentReactionProcessor.unlikeComment(1L, 101L);

        // then
        assertThat(target.commentId()).isEqualTo(101L);
        verify(commentReader).readReactionTarget(101L);
        verify(commentReactionWriter).unlikeComment(1L, 101L);
        verify(commentWriter).decreaseLikeCount(101L);
    }
}
