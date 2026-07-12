package cluverse.post.service.request;

import cluverse.post.domain.PostCategory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V4(날짜 앵커 + 커서) 목록 조회 요청.
 * - 진입: date(앵커)로 해당 날짜 이하 최신순 진입. date가 없으면 최신부터.
 * - 이동: 응답의 prevCursor/nextCursor(createdAt, postId)를 그대로 넘긴다.
 * 커서를 opaque 문자열로 감추지 않고 두 필드로 노출한 것은 측정/디버깅 가시성을 위한 선택이다.
 */
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

    /**
     * 날짜 앵커 진입 조건의 상한(exclusive). created_at < date+1일 00:00은
     * (created_at, post_id) <= (date 23:59:59.999..., MAX)와 동치이면서 정밀도 이슈가 없다.
     */
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
