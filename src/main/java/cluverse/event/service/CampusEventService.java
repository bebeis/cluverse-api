package cluverse.event.service;

import cluverse.event.service.implement.CampusEventReader;
import cluverse.event.service.request.CampusEventSearchRequest;
import cluverse.event.service.response.CampusEventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampusEventService {

    private final CampusEventReader campusEventReader;

    public List<CampusEventResponse> getEvents(CampusEventSearchRequest request) {
        LocalDate today = LocalDate.now();
        return campusEventReader.readEvents(request).stream()
                .map(event -> CampusEventResponse.from(event, today))
                .toList();
    }

    public CampusEventResponse getEvent(Long eventId) {
        return CampusEventResponse.from(campusEventReader.readOrThrow(eventId), LocalDate.now());
    }
}
