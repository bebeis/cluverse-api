package cluverse.certification.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record CertificationSchedule(
        String qualificationType,
        String description,
        LocalDate writtenRegistrationStartDate,
        LocalDate writtenRegistrationEndDate,
        LocalDate writtenExamStartDate,
        LocalDate writtenExamEndDate,
        LocalDate practicalRegistrationStartDate,
        LocalDate practicalRegistrationEndDate,
        LocalDate practicalExamStartDate,
        LocalDate practicalExamEndDate
) {

    public List<CertificationDeadline> registrationDeadlinesOnOrAfter(LocalDate date) {
        List<CertificationDeadline> deadlines = new ArrayList<>();
        if (isOnOrAfter(writtenRegistrationEndDate, date)) {
            deadlines.add(new CertificationDeadline(
                    qualificationType,
                    description,
                    CertificationExamPhase.WRITTEN,
                    writtenRegistrationStartDate,
                    writtenRegistrationEndDate,
                    writtenExamStartDate,
                    writtenExamEndDate
            ));
        }
        if (isOnOrAfter(practicalRegistrationEndDate, date)) {
            deadlines.add(new CertificationDeadline(
                    qualificationType,
                    description,
                    CertificationExamPhase.PRACTICAL,
                    practicalRegistrationStartDate,
                    practicalRegistrationEndDate,
                    practicalExamStartDate,
                    practicalExamEndDate
            ));
        }
        return List.copyOf(deadlines);
    }

    private boolean isOnOrAfter(LocalDate registrationEndDate, LocalDate date) {
        return registrationEndDate != null && !registrationEndDate.isBefore(date);
    }
}
