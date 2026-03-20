package cluverse.calendar.service.implement;

import cluverse.calendar.domain.CalendarEvent;
import cluverse.calendar.exception.CalendarExceptionMessage;
import cluverse.calendar.repository.CalendarEventRepository;
import cluverse.calendar.service.request.CalendarEventSearchRequest;
import cluverse.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarEventReader {

    private final CalendarEventRepository calendarEventRepository;

    public List<CalendarEvent> readEvents(Long memberId, CalendarEventSearchRequest request) {
        return calendarEventRepository.search(memberId, request.from(), request.to(), request.category());
    }

    public List<CalendarEvent> readUpcomingEvents(Long memberId, int size) {
        return calendarEventRepository.findAllByMemberIdAndEndAtGreaterThanEqualOrderByStartAtAscIdAsc(
                memberId,
                LocalDateTime.now(),
                PageRequest.of(0, size)
        );
    }

    public CalendarEvent readOrThrow(Long eventId) {
        return calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(CalendarExceptionMessage.EVENT_NOT_FOUND.getMessage()));
    }
}
