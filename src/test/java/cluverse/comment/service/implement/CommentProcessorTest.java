package cluverse.comment.service.implement;

import cluverse.comment.domain.Comment;
import cluverse.comment.service.request.CommentCreateRequest;
import cluverse.common.exception.ForbiddenException;
import cluverse.member.service.implement.MemberReader;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.service.implement.PostAccessReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentProcessorTest {

    @Mock
    private CommentReader commentReader;

    @Mock
    private CommentWriter commentWriter;

    @Mock
    private MemberReader memberReader;

    @Mock
    private PostAccessReader postAccessReader;

    @Mock
    private PostMetaWriter postMetaWriter;

    @InjectMocks
    private CommentProcessor commentProcessor;

    @Test
    void 대댓글_작성시_게시글_댓글수와_부모_대댓글수를_증가시킨다() {
        // given
        CommentCreateRequest request = new CommentCreateRequest(100L, "대댓글입니다.", false);
        Comment parentComment = createComment(100L, 10L, 2L, null, 0);
        Comment createdComment = createComment(101L, 10L, 1L, 100L, 1);
        when(commentReader.readOrThrow(100L)).thenReturn(parentComment);
        when(commentWriter.create(1L, 10L, parentComment, request, "127.0.0.1")).thenReturn(createdComment);

        // when
        Long commentId = commentProcessor.createComment(1L, 10L, request, "127.0.0.1");

        // then
        assertThat(commentId).isEqualTo(101L);
        verify(postAccessReader).validateWritablePost(1L, 10L);
        verify(postMetaWriter).increaseCommentCount(10L);
        verify(commentWriter).increaseReplyCount(100L);
    }

    @Test
    void 작성자도_관리자도_아니면_댓글을_삭제할_수_없다() {
        // given
        Comment comment = createComment(101L, 10L, 2L, null, 0);
        when(commentReader.readOrThrow(101L)).thenReturn(comment);
        when(memberReader.isAdmin(1L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> commentProcessor.deleteComment(1L, 101L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void 자식이_있는_댓글을_삭제하면_soft_delete만_수행한다() {
        // given
        Comment comment = createComment(101L, 10L, 1L, null, 0);
        when(commentReader.readOrThrow(101L)).thenReturn(comment);
        when(commentReader.hasChildren(comment)).thenReturn(true);

        // when
        Long postId = commentProcessor.deleteComment(1L, 101L);

        // then
        assertThat(postId).isEqualTo(10L);
        verify(commentWriter).delete(comment);
        verify(commentWriter, never()).remove(any());
        verify(postMetaWriter, never()).decreaseCommentCount(anyLong());
    }

    @Test
    void 자식이_없는_삭제된_부모는_자식_삭제시_함께_실제_삭제된다() {
        // given
        Comment child = createComment(102L, 10L, 1L, 101L, 1);
        Comment deletedParent = createDeletedComment(101L, 10L, 1L, null, 0);
        when(commentReader.readOrThrow(102L)).thenReturn(child);
        when(commentReader.hasChildren(child)).thenReturn(false);
        when(commentReader.read(101L)).thenReturn(Optional.of(deletedParent));
        when(commentReader.hasChildren(deletedParent)).thenReturn(false);

        // when
        commentProcessor.deleteComment(1L, 102L);

        // then
        verify(commentWriter).remove(child);
        verify(commentWriter).decreaseReplyCount(101L);
        verify(commentWriter).remove(deletedParent);
        verify(postMetaWriter, times(2)).decreaseCommentCount(10L);
    }

    private Comment createComment(Long commentId, Long postId, Long memberId, Long parentId, int depth) {
        Comment comment = Comment.createByMember(postId, memberId, parentId, depth, "댓글 내용", false, "127.0.0.1");
        ReflectionTestUtils.setField(comment, "id", commentId);
        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.of(2026, 3, 15, 10, 0));
        ReflectionTestUtils.setField(comment, "updatedAt", LocalDateTime.of(2026, 3, 15, 10, 0));
        return comment;
    }

    private Comment createDeletedComment(Long commentId, Long postId, Long memberId, Long parentId, int depth) {
        Comment comment = createComment(commentId, postId, memberId, parentId, depth);
        comment.delete();
        return comment;
    }
}
