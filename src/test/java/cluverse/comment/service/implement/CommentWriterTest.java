package cluverse.comment.service.implement;

import cluverse.comment.domain.Comment;
import cluverse.comment.repository.CommentRepository;
import cluverse.comment.service.request.CommentCreateRequest;
import cluverse.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CommentWriterTest {

    @Mock
    private CommentRepository commentRepository;

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
}
