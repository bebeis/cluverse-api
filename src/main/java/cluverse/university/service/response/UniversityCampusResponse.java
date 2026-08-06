package cluverse.university.service.response;

import cluverse.university.domain.UniversityCampus;

import java.math.BigDecimal;

public record UniversityCampusResponse(
        Long campusId,
        Long universityId,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        int localRadiusMeter
) {
    public static UniversityCampusResponse from(UniversityCampus campus) {
        return new UniversityCampusResponse(
                campus.getId(),
                campus.getUniversityId(),
                campus.getName(),
                campus.getLatitude(),
                campus.getLongitude(),
                campus.getLocalRadiusMeter()
        );
    }
}
