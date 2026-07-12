package cluverse.comment.service.implement;

import cluverse.comment.domain.Comment;
import cluverse.comment.repository.CommentRepository;
import cluverse.comment.service.request.CommentCreateRequest;
import cluverse.comment.service.request.CommentUpdateRequest;
import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentWriterTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentReader commentReader;

    @InjectMocks
    private CommentWriter commentWriter;

    @Test
    void 댓글_depth는_5를_초과할_수_없다() {
        // given
        Comment parentComment = Comment.createByMember(10L, 1L, 100L, 5, "부모 댓글", false, "127.0.0.1");
        CommentCreateRequest request = new CommentCreateRequest(parentComment.getId(), "대댓글", false);

        // when & then
        assertThatThrownBy(() -> commentWriter.create(2L, 10L, parentComment, request, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 작성자는_댓글을_수정한다() {
        // given
        Comment comment = createComment(101L, 1L);
        when(commentReader.readActiveOrThrow(101L)).thenReturn(comment);

        // when
        commentWriter.update(1L, 101L, new CommentUpdateRequest("수정된 댓글입니다."));

        // then
        assertThat(comment.getContent()).isEqualTo("수정된 댓글입니다.");
    }

    @Test
    void 작성자가_아니면_댓글을_수정할_수_없다() {
        // given
        Comment comment = createComment(101L, 2L);
        when(commentReader.readActiveOrThrow(101L)).thenReturn(comment);

        // when & then
        assertThatThrownBy(() -> commentWriter.update(1L, 101L, new CommentUpdateRequest("수정")))
                .isInstanceOf(ForbiddenException.class);
    }

    private Comment createComment(Long commentId, Long memberId) {
        Comment comment = Comment.createByMember(10L, memberId, null, 0, "원본 댓글", false, "127.0.0.1");
        ReflectionTestUtils.setField(comment, "id", commentId);
        return comment;
    }
}
