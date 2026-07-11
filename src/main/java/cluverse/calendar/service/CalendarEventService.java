package cluverse.calendar.service;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.calendar.service.implement.CalendarEventWriter;
import cluverse.calendar.service.request.CalendarEventCreateRequest;
import cluverse.calendar.service.request.CalendarEventUpdateRequest;
import cluverse.calendar.service.response.CalendarEventResponse;
import cluverse.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventWriter calendarEventWriter;

    public CalendarEventResponse createEvent(Long memberId, CalendarEventCreateRequest request) {
        validateAuthenticated(memberId);
        return CalendarEventResponse.from(calendarEventWriter.create(memberId, request));
    }

    public CalendarEventResponse updateEvent(Long memberId, Long eventId, CalendarEventUpdateRequest request) {
        validateAuthenticated(memberId);
        return CalendarEventResponse.from(calendarEventWriter.update(memberId, eventId, request));
    }

    public void deleteEvent(Long memberId, Long eventId) {
        validateAuthenticated(memberId);
        calendarEventWriter.delete(memberId, eventId);
    }

    private void validateAuthenticated(Long memberId) {
        if (memberId == null) {
            throw new UnauthorizedException(AuthExceptionMessage.UNAUTHORIZED.getMessage());
        }
    }
}
