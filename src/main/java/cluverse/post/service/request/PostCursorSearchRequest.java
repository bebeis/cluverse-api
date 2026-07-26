package cluverse.post.service.request;

import cluverse.post.domain.PostCategory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PostCursorSearchRequest(
        @NotNull(message = "게시판 ID를 입력해주세요.")
        Long boardId,

        PostCategory category,

        @Min(value = 1, message = "조회 건수는 1 이상이어야 합니다.")
        @Max(value = 100, message = "조회 건수는 100 이하여야 합니다.")
        Integer size,

        LocalDate date,

        LocalDateTime cursorCreatedAt,

        Long cursorPostId,

        PostCursorDirection direction
) {
    private static final int DEFAULT_SIZE = 20;
    private static final PostCursorDirection DEFAULT_DIRECTION = PostCursorDirection.NEXT;

    public int sizeOrDefault() {
        return size == null ? DEFAULT_SIZE : size;
    }

    public PostCursorDirection directionOrDefault() {
        return direction == null ? DEFAULT_DIRECTION : direction;
    }

    public boolean hasCursor() {
        return cursorCreatedAt != null && cursorPostId != null;
    }

    public boolean isDateAnchored() {
        return date != null;
    }
    
    public LocalDateTime exclusiveDateEnd() {
        return date.plusDays(1).atStartOfDay();
    }

    @AssertTrue(message = "커서는 cursorCreatedAt과 cursorPostId를 함께 입력해야 합니다.")
    public boolean isCursorPairComplete() {
        return (cursorCreatedAt == null) == (cursorPostId == null);
    }

    @AssertTrue(message = "날짜 진입과 커서 이동은 함께 사용할 수 없습니다.")
    public boolean isDateCursorExclusive() {
        return date == null || (cursorCreatedAt == null && cursorPostId == null);
    }

    @AssertTrue(message = "이동 방향(direction)은 커서와 함께 사용해야 합니다.")
    public boolean isDirectionWithCursor() {
        return direction == null || hasCursor();
    }
}
