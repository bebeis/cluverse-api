package cluverse.event.service.request;

import java.time.LocalDate;

public record CampusEventSearchRequest(
        Boolean ongoing,
        LocalDate from,
        LocalDate to
) {
    public boolean ongoingOrDefault() {
        return Boolean.TRUE.equals(ongoing);
    }
}
