package cluverse.event.controller;

import cluverse.docs.RestDocsSupport;
import cluverse.event.service.CampusEventService;
import cluverse.event.service.response.CampusEventResponse;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CampusEventControllerDocsTest extends RestDocsSupport {

    private final CampusEventService campusEventService = mock(CampusEventService.class);

    @Override
    protected Object initController() {
        return new CampusEventController(campusEventService);
    }

    @Test
    void 행사_목록_조회() throws Exception {
        when(campusEventService.getEvents(any())).thenReturn(List.of(createResponse()));

        mockMvc.perform(get("/api/v1/events")
                        .queryParam("ongoing", "true")
                        .queryParam("from", "2026-03-01")
                        .queryParam("to", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].eventId").value(1L))
                .andDo(document("events/get-list",
                        queryParameters(
                                parameterWithName("ongoing").description("진행 중 행사만 조회할지 여부").optional(),
                                parameterWithName("from").description("조회 시작일").optional(),
                                parameterWithName("to").description("조회 종료일").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data[].eventId").type(JsonFieldType.NUMBER).description("행사 ID"),
                                fieldWithPath("data[].title").type(JsonFieldType.STRING).description("행사명"),
                                fieldWithPath("data[].host").type(JsonFieldType.STRING).description("주최"),
                                fieldWithPath("data[].startDate").type(JsonFieldType.STRING).description("시작일"),
                                fieldWithPath("data[].endDate").type(JsonFieldType.STRING).description("종료일"),
                                fieldWithPath("data[].location").type(JsonFieldType.STRING).description("장소").optional(),
                                fieldWithPath("data[].thumbnailImageUrl").type(JsonFieldType.STRING).description("썸네일 URL").optional(),
                                fieldWithPath("data[].isOngoing").type(JsonFieldType.BOOLEAN).description("진행 중 여부"),
                                fieldWithPath("data[].summary").type(JsonFieldType.STRING).description("요약").optional()
                        )
                ));
    }

    @Test
    void 행사_상세_조회() throws Exception {
        when(campusEventService.getEvent(1L)).thenReturn(createResponse());

        mockMvc.perform(get("/api/v1/events/{eventId}", 1L))
                .andExpect(status().isOk())
                .andDo(document("events/get-detail",
                        pathParameters(
                                parameterWithName("eventId").description("행사 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.eventId").type(JsonFieldType.NUMBER).description("행사 ID"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING).description("행사명"),
                                fieldWithPath("data.host").type(JsonFieldType.STRING).description("주최"),
                                fieldWithPath("data.startDate").type(JsonFieldType.STRING).description("시작일"),
                                fieldWithPath("data.endDate").type(JsonFieldType.STRING).description("종료일"),
                                fieldWithPath("data.location").type(JsonFieldType.STRING).description("장소").optional(),
                                fieldWithPath("data.thumbnailImageUrl").type(JsonFieldType.STRING).description("썸네일 URL").optional(),
                                fieldWithPath("data.isOngoing").type(JsonFieldType.BOOLEAN).description("진행 중 여부"),
                                fieldWithPath("data.summary").type(JsonFieldType.STRING).description("요약").optional()
                        )
                ));
    }

    private CampusEventResponse createResponse() {
        return new CampusEventResponse(
                1L,
                "2026 봄 축제",
                "학생처",
                LocalDate.of(2026, 3, 24),
                LocalDate.of(2026, 3, 26),
                "대운동장",
                "https://cdn.example.com/events/spring-festival.png",
                true,
                "공연과 플리마켓이 함께 열리는 봄 축제"
        );
    }
}
