package cluverse.calendar.service;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.calendar.service.implement.CalendarEventReader;
import cluverse.calendar.service.request.CalendarEventSearchRequest;
import cluverse.calendar.service.response.CalendarEventResponse;
import cluverse.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarEventQueryService {

    private final CalendarEventReader calendarEventReader;

    public List<CalendarEventResponse> getEvents(Long memberId, CalendarEventSearchRequest request) {
        validateAuthenticated(memberId);
        return calendarEventReader.readEvents(memberId, request).stream()
                .map(CalendarEventResponse::from)
                .toList();
    }

    public List<CalendarEventResponse> getUpcomingEvents(Long memberId, int size) {
        validateAuthenticated(memberId);
        return calendarEventReader.readUpcomingEvents(memberId, size).stream()
                .map(CalendarEventResponse::from)
                .toList();
    }

    private void validateAuthenticated(Long memberId) {
        if (memberId == null) {
            throw new UnauthorizedException(AuthExceptionMessage.UNAUTHORIZED.getMessage());
        }
    }
}
