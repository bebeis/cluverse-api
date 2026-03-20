package cluverse.calendar.service.response;

import cluverse.calendar.domain.CalendarEvent;
import cluverse.calendar.domain.CalendarEventCategory;
import cluverse.calendar.domain.CalendarEventVisibility;

import java.time.LocalDateTime;

public record CalendarEventResponse(
        Long eventId,
        String title,
        String description,
        CalendarEventCategory category,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String location,
        boolean allDay,
        CalendarEventVisibility visibility
) {
    public static CalendarEventResponse from(CalendarEvent event) {
        return new CalendarEventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.getStartAt(),
                event.getEndAt(),
                event.getLocation(),
                event.isAllDay(),
                event.getVisibility()
        );
    }
}
