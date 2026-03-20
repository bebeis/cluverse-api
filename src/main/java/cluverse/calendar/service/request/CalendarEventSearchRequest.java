package cluverse.calendar.service.request;

import cluverse.calendar.domain.CalendarEventCategory;

import java.time.LocalDateTime;

public record CalendarEventSearchRequest(
        LocalDateTime from,
        LocalDateTime to,
        CalendarEventCategory category
) {
}
