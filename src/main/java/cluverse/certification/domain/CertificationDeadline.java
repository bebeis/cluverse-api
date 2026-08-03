package cluverse.certification.domain;

import java.time.LocalDate;

public record CertificationDeadline(
        String qualificationType,
        String description,
        CertificationExamPhase phase,
        LocalDate registrationStartDate,
        LocalDate registrationEndDate,
        LocalDate examStartDate,
        LocalDate examEndDate
) {
}
