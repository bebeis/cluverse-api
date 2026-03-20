package cluverse.event.service.response;

import cluverse.event.domain.CampusEvent;

import java.time.LocalDate;

public record CampusEventResponse(
        Long eventId,
        String title,
        String host,
        LocalDate startDate,
        LocalDate endDate,
        String location,
        String thumbnailImageUrl,
        boolean isOngoing,
        String summary
) {
    public static CampusEventResponse from(CampusEvent event, LocalDate today) {
        return new CampusEventResponse(
                event.getId(),
                event.getTitle(),
                event.getHost(),
                event.getStartDate(),
                event.getEndDate(),
                event.getLocation(),
                event.getThumbnailImageUrl(),
                !event.getStartDate().isAfter(today) && !event.getEndDate().isBefore(today),
                event.getSummary()
        );
    }
}
