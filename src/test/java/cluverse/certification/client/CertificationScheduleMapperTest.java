package cluverse.certification.client;

import cluverse.certification.domain.CertificationSchedule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.JsonNodeFactory;

import static org.assertj.core.api.Assertions.assertThat;

class CertificationScheduleMapperTest {

    private final CertificationScheduleMapper mapper = new CertificationScheduleMapper();

    @Test
    void 공공_API의_기본_날짜_문자열을_일정으로_변환한다() {
        // given
        DataGoKrCertificationResponse response = response(new DataGoKrCertificationResponse.Item(
                "국가기술자격",
                "2026년 정기 기사 1회",
                "20260801",
                "20260803",
                "20260820",
                "20260821",
                "",
                null,
                null,
                null
        ));

        // when
        List<CertificationSchedule> schedules = mapper.map(response);

        // then
        assertThat(schedules).singleElement().satisfies(schedule -> {
            assertThat(schedule.writtenRegistrationStartDate()).isEqualTo(LocalDate.of(2026, 8, 1));
            assertThat(schedule.writtenRegistrationEndDate()).isEqualTo(LocalDate.of(2026, 8, 3));
            assertThat(schedule.practicalRegistrationStartDate()).isNull();
        });
    }

    @Test
    void 항목이_없는_응답은_빈_목록으로_변환한다() {
        assertThat(mapper.map(new DataGoKrCertificationResponse(
                new DataGoKrCertificationResponse.Response(
                        new DataGoKrCertificationResponse.Header("00", "NORMAL SERVICE"),
                        new DataGoKrCertificationResponse.Body(null)
                )
        ))).isEmpty();
    }

    private DataGoKrCertificationResponse response(DataGoKrCertificationResponse.Item item) {
        ArrayNode items = JsonNodeFactory.instance.arrayNode();
        ObjectNode node = items.addObject();
        node.put("qualgbNm", item.qualgbNm());
        node.put("description", item.description());
        put(node, "docRegStartDt", item.docRegStartDt());
        put(node, "docRegEndDt", item.docRegEndDt());
        put(node, "docExamStartDt", item.docExamStartDt());
        put(node, "docExamEndDt", item.docExamEndDt());
        put(node, "pracRegStartDt", item.pracRegStartDt());
        put(node, "pracRegEndDt", item.pracRegEndDt());
        put(node, "pracExamStartDt", item.pracExamStartDt());
        put(node, "pracExamEndDt", item.pracExamEndDt());
        return new DataGoKrCertificationResponse(
                new DataGoKrCertificationResponse.Response(
                        new DataGoKrCertificationResponse.Header("00", "NORMAL SERVICE"),
                        new DataGoKrCertificationResponse.Body(items)
                )
        );
    }

    private void put(ObjectNode node, String fieldName, String value) {
        if (value == null) {
            node.putNull(fieldName);
        } else {
            node.put(fieldName, value);
        }
    }
}
