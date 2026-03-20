package cluverse.event.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.event.domain.CampusEvent;
import cluverse.event.exception.EventExceptionMessage;
import cluverse.event.repository.CampusEventRepository;
import cluverse.event.service.request.CampusEventSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampusEventReader {

    private final CampusEventRepository campusEventRepository;

    public List<CampusEvent> readEvents(CampusEventSearchRequest request) {
        return campusEventRepository.search(LocalDate.now(), request.ongoingOrDefault(), request.from(), request.to());
    }

    public CampusEvent readOrThrow(Long eventId) {
        return campusEventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(EventExceptionMessage.EVENT_NOT_FOUND.getMessage()));
    }
}
