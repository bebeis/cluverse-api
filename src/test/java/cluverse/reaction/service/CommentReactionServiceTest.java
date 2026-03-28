package cluverse.reaction.service;

import cluverse.comment.service.response.CommentReactionTargetResponse;
import cluverse.comment.service.implement.CommentReader;
import cluverse.comment.service.implement.CommentWriter;
import cluverse.reaction.service.implement.CommentReactionWriter;
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
    private CommentReactionWriter commentReactionWriter;

    @Mock
    private CommentReader commentReader;

    @Mock
    private CommentWriter commentWriter;

    @InjectMocks
    private CommentReactionService commentReactionService;

    @Test
    void 댓글_좋아요시_댓글_검증후_좋아요_수까지_증가시킨다() {
        // given
        when(commentReader.readReactionTarget(101L)).thenReturn(new CommentReactionTargetResponse(10L, 101L));

        // when
        CommentLikeResponse response = commentReactionService.likeComment(1L, 101L);

        // then
        assertThat(response.postId()).isEqualTo(10L);
        assertThat(response.commentId()).isEqualTo(101L);
        assertThat(response.liked()).isTrue();
        verify(commentReader).readReactionTarget(101L);
        verify(commentReactionWriter).likeComment(1L, 101L);
        verify(commentWriter).increaseLikeCount(101L);
    }
}
