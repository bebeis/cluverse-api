package cluverse.calendar.service;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.calendar.domain.CalendarEvent;
import cluverse.calendar.exception.CalendarExceptionMessage;
import cluverse.calendar.service.implement.CalendarEventReader;
import cluverse.calendar.service.implement.CalendarEventWriter;
import cluverse.calendar.service.request.CalendarEventCreateRequest;
import cluverse.calendar.service.request.CalendarEventSearchRequest;
import cluverse.calendar.service.request.CalendarEventUpdateRequest;
import cluverse.calendar.service.response.CalendarEventResponse;
import cluverse.common.exception.ForbiddenException;
import cluverse.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CalendarEventService {

    private final CalendarEventReader calendarEventReader;
    private final CalendarEventWriter calendarEventWriter;

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getEvents(Long memberId, CalendarEventSearchRequest request) {
        validateAuthenticated(memberId);
        return calendarEventReader.readEvents(memberId, request).stream()
                .map(CalendarEventResponse::from)
                .toList();
    }

    public CalendarEventResponse createEvent(Long memberId, CalendarEventCreateRequest request) {
        validateAuthenticated(memberId);
        return CalendarEventResponse.from(calendarEventWriter.create(memberId, request));
    }

    public CalendarEventResponse updateEvent(Long memberId, Long eventId, CalendarEventUpdateRequest request) {
        validateAuthenticated(memberId);
        CalendarEvent event = readOwnedEvent(memberId, eventId);
        calendarEventWriter.update(event, request);
        return CalendarEventResponse.from(event);
    }

    public void deleteEvent(Long memberId, Long eventId) {
        validateAuthenticated(memberId);
        calendarEventWriter.delete(readOwnedEvent(memberId, eventId));
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getUpcomingEvents(Long memberId, int size) {
        validateAuthenticated(memberId);
        return calendarEventReader.readUpcomingEvents(memberId, size).stream()
                .map(CalendarEventResponse::from)
                .toList();
    }

    private CalendarEvent readOwnedEvent(Long memberId, Long eventId) {
        CalendarEvent event = calendarEventReader.readOrThrow(eventId);
        if (!event.isOwner(memberId)) {
            throw new ForbiddenException(CalendarExceptionMessage.EVENT_ACCESS_DENIED.getMessage());
        }
        return event;
    }

    private void validateAuthenticated(Long memberId) {
        if (memberId == null) {
            throw new UnauthorizedException(AuthExceptionMessage.UNAUTHORIZED.getMessage());
        }
    }
}
