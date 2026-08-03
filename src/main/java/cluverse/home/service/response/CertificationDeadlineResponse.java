package cluverse.home.service.response;

import cluverse.certification.domain.CertificationDeadline;
import cluverse.certification.domain.CertificationExamPhase;

import java.time.LocalDate;

public record CertificationDeadlineResponse(
        String qualificationType,
        String description,
        CertificationExamPhase phase,
        LocalDate registrationStartDate,
        LocalDate registrationEndDate,
        LocalDate examStartDate,
        LocalDate examEndDate
) {
    public static CertificationDeadlineResponse from(CertificationDeadline deadline) {
        return new CertificationDeadlineResponse(
                deadline.qualificationType(),
                deadline.description(),
                deadline.phase(),
                deadline.registrationStartDate(),
                deadline.registrationEndDate(),
                deadline.examStartDate(),
                deadline.examEndDate()
        );
    }
}
