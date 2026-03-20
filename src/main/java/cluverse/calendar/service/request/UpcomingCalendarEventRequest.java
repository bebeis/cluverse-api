package cluverse.calendar.service.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpcomingCalendarEventRequest(
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 20, message = "size는 20 이하여야 합니다.")
        Integer size
) {
    private static final int DEFAULT_SIZE = 5;

    public int sizeOrDefault() {
        return size == null ? DEFAULT_SIZE : size;
    }
}
