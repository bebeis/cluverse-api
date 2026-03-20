package cluverse.calendar.service.implement;

import cluverse.calendar.domain.CalendarEvent;
import cluverse.calendar.exception.CalendarExceptionMessage;
import cluverse.calendar.repository.CalendarEventRepository;
import cluverse.calendar.service.request.CalendarEventCreateRequest;
import cluverse.calendar.service.request.CalendarEventUpdateRequest;
import cluverse.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Transactional
public class CalendarEventWriter {

    private final CalendarEventRepository calendarEventRepository;

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

    public void update(CalendarEvent event, CalendarEventUpdateRequest request) {
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
    }

    public void delete(CalendarEvent event) {
        calendarEventRepository.delete(event);
    }

    private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (endAt.isBefore(startAt)) {
            throw new BadRequestException(CalendarExceptionMessage.INVALID_EVENT_PERIOD.getMessage());
        }
    }
}
