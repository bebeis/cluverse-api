package cluverse.certification.client;

import cluverse.certification.domain.CertificationSchedule;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
public class CertificationScheduleMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    public List<CertificationSchedule> map(DataGoKrCertificationResponse response) {
        if (response == null
                || response.response() == null
                || response.response().body() == null
                || response.response().body().items() == null
                || response.response().body().items().item() == null) {
            return List.of();
        }
        return response.response().body().items().item().stream()
                .map(this::map)
                .toList();
    }

    private CertificationSchedule map(DataGoKrCertificationResponse.Item item) {
        return new CertificationSchedule(
                item.qualgbNm(),
                item.description(),
                parseDate(item.docRegStartDt()),
                parseDate(item.docRegEndDt()),
                parseDate(item.docExamStartDt()),
                parseDate(item.docExamEndDt()),
                parseDate(item.pracRegStartDt()),
                parseDate(item.pracRegEndDt()),
                parseDate(item.pracExamStartDt()),
                parseDate(item.pracExamEndDt())
        );
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
