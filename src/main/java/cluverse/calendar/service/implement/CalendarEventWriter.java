package cluverse.calendar.service.implement;

import cluverse.calendar.domain.CalendarEvent;
import cluverse.calendar.exception.CalendarExceptionMessage;
import cluverse.calendar.repository.CalendarEventRepository;
import cluverse.calendar.service.request.CalendarEventCreateRequest;
import cluverse.calendar.service.request.CalendarEventUpdateRequest;
import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Transactional
public class CalendarEventWriter {

    private final CalendarEventRepository calendarEventRepository;
    private final CalendarEventReader calendarEventReader;

    public CalendarEvent create(Long memberId, CalendarEventCreateRequest request) {
        validatePeriod(request.startAt(), request.endAt());
        return calendarEventRepository.save(CalendarEvent.create(
                memberId,
                request.title(),
                request.description(),
                request.category(),
                request.startAt(),
                request.endAt(),
                request.location(),
                request.allDay(),
                request.visibility()
        ));
    }

    public CalendarEvent update(Long memberId, Long eventId, CalendarEventUpdateRequest request) {
        CalendarEvent event = readOwnedEvent(memberId, eventId);
        validatePeriod(request.startAt(), request.endAt());
        event.update(
                request.title(),
                request.description(),
                request.category(),
                request.startAt(),
                request.endAt(),
                request.location(),
                request.allDay(),
                request.visibility()
        );
        return event;
    }

    public void delete(Long memberId, Long eventId) {
        calendarEventRepository.delete(readOwnedEvent(memberId, eventId));
    }

    private CalendarEvent readOwnedEvent(Long memberId, Long eventId) {
        CalendarEvent event = calendarEventReader.readOrThrow(eventId);
        if (!event.isOwner(memberId)) {
            throw new ForbiddenException(CalendarExceptionMessage.EVENT_ACCESS_DENIED.getMessage());
        }
        return event;
    }

    private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (endAt.isBefore(startAt)) {
            throw new BadRequestException(CalendarExceptionMessage.INVALID_EVENT_PERIOD.getMessage());
        }
    }
}
