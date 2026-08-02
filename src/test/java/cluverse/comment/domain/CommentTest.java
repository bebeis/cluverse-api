package cluverse.comment.domain;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentTest {

    @Test
    void 부모_경로를_포함한_path가_255자를_초과하면_거부한다() {
        // given
        Comment parent = savedComment(1L);
        ReflectionTestUtils.setField(parent, "path", "a".repeat(255));
        Comment child = savedComment(2L);

        // when & then
        assertThatThrownBy(() -> child.assignPath(parent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("댓글 path 길이는 255자를 초과할 수 없습니다.");
    }

    private Comment savedComment(Long commentId) {
        Comment comment = Comment.createByMember(10L, 1L, null, 0, "댓글", false, "127.0.0.1");
        ReflectionTestUtils.setField(comment, "id", commentId);
        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.of(2026, 8, 2, 10, 30, 15));
        return comment;
    }
}
