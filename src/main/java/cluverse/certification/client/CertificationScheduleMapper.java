package cluverse.certification.client;

import cluverse.certification.domain.CertificationSchedule;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
public class CertificationScheduleMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    public List<CertificationSchedule> map(DataGoKrCertificationResponse response) {
        if (response == null
                || response.resolvedBody() == null
                || response.resolvedBody().items() == null) {
            return List.of();
        }
        JsonNode itemsNode = response.resolvedBody().items();
        JsonNode itemArray = itemsNode.isArray() ? itemsNode : itemsNode.get("item");
        if (itemArray == null || !itemArray.isArray()) {
            return List.of();
        }
        List<CertificationSchedule> schedules = new java.util.ArrayList<>();
        for (JsonNode item : itemArray) {
            schedules.add(map(new DataGoKrCertificationResponse.Item(
                    text(item, "qualgbNm"),
                    text(item, "description"),
                    text(item, "docRegStartDt"),
                    text(item, "docRegEndDt"),
                    text(item, "docExamStartDt"),
                    text(item, "docExamEndDt"),
                    text(item, "pracRegStartDt"),
                    text(item, "pracRegEndDt"),
                    text(item, "pracExamStartDt"),
                    text(item, "pracExamEndDt")
            )));
        }
        return List.copyOf(schedules);
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.stringValue(null);
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
