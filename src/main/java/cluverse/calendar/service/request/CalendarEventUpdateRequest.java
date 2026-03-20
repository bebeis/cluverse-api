package cluverse.calendar.service.request;

import cluverse.calendar.domain.CalendarEventCategory;
import cluverse.calendar.domain.CalendarEventVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CalendarEventUpdateRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        @Size(max = 2000, message = "설명은 2000자 이하여야 합니다.")
        String description,

        @NotNull(message = "카테고리를 입력해주세요.")
        CalendarEventCategory category,

        @NotNull(message = "시작 시각을 입력해주세요.")
        LocalDateTime startAt,

        @NotNull(message = "종료 시각을 입력해주세요.")
        LocalDateTime endAt,

        @Size(max = 255, message = "장소는 255자 이하여야 합니다.")
        String location,

        boolean allDay,

        @NotNull(message = "공개 범위를 입력해주세요.")
        CalendarEventVisibility visibility
) {
}
