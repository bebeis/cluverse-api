package cluverse.comment.domain;

import cluverse.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentPageCursorTest {

    @Test
    void 댓글_커서를_인코딩하고_복원한다() {
        // given
        CommentPageCursor cursor = new CommentPageCursor(
                "20260802103015-00000000000000000100",
                LocalDateTime.of(2026, 8, 2, 10, 30, 15),
                100L
        );

        // when
        CommentPageCursor decoded = CommentPageCursor.decode(cursor.encode());

        // then
        assertThat(decoded).isEqualTo(cursor);
    }

    @Test
    void 형식이_잘못된_댓글_커서를_거부한다() {
        assertThatThrownBy(() -> CommentPageCursor.decode("invalid-cursor"))
                .isInstanceOf(BadRequestException.class);
    }
}
