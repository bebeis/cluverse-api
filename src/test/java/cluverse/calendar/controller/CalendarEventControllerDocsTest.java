package cluverse.calendar.controller;

import cluverse.calendar.domain.CalendarEventCategory;
import cluverse.calendar.domain.CalendarEventVisibility;
import cluverse.calendar.service.CalendarEventService;
import cluverse.calendar.service.CalendarEventQueryService;
import cluverse.calendar.service.request.CalendarEventCreateRequest;
import cluverse.calendar.service.request.CalendarEventUpdateRequest;
import cluverse.calendar.service.response.CalendarEventResponse;
import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static cluverse.common.auth.LoginMemberArgumentResolver.SESSION_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CalendarEventControllerDocsTest extends RestDocsSupport {

    private final CalendarEventQueryService calendarEventQueryService = mock(CalendarEventQueryService.class);
    private final CalendarEventService calendarEventService = mock(CalendarEventService.class);

    @Override
    protected Object initController() {
        return new CalendarEventController(calendarEventQueryService, calendarEventService);
    }

    @Test
    void 일정_목록_조회() throws Exception {
        when(calendarEventQueryService.getEvents(anyLong(), any())).thenReturn(List.of(createResponse()));

        mockMvc.perform(get("/api/v1/calendar/events")
                        .session(createSession())
                        .queryParam("from", "2026-03-01T00:00:00")
                        .queryParam("to", "2026-03-31T23:59:59")
                        .queryParam("category", "PERSONAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].eventId").value(1L))
                .andDo(document("calendar-events/get-list",
                        queryParameters(
                                parameterWithName("from").description("조회 시작 시각").optional(),
                                parameterWithName("to").description("조회 종료 시각").optional(),
                                parameterWithName("category").description("일정 카테고리").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data[].eventId").type(JsonFieldType.NUMBER).description("일정 ID"),
                                fieldWithPath("data[].title").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("data[].description").type(JsonFieldType.STRING).description("설명").optional(),
                                fieldWithPath("data[].category").type(JsonFieldType.STRING).description("카테고리"),
                                fieldWithPath("data[].startAt").type(JsonFieldType.STRING).description("시작 시각"),
                                fieldWithPath("data[].endAt").type(JsonFieldType.STRING).description("종료 시각"),
                                fieldWithPath("data[].location").type(JsonFieldType.STRING).description("장소").optional(),
                                fieldWithPath("data[].allDay").type(JsonFieldType.BOOLEAN).description("종일 여부"),
                                fieldWithPath("data[].visibility").type(JsonFieldType.STRING).description("공개 범위")
                        )
                ));
    }

    @Test
    void 일정_생성() throws Exception {
        when(calendarEventService.createEvent(anyLong(), any(CalendarEventCreateRequest.class))).thenReturn(createResponse());

        mockMvc.perform(post("/api/v1/calendar/events")
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "캡스톤 회의",
                                  "description": "주간 진행 상황 점검",
                                  "category": "GROUP",
                                  "startAt": "2026-03-21T14:00:00",
                                  "endAt": "2026-03-21T15:30:00",
                                  "location": "공학관 301호",
                                  "allDay": false,
                                  "visibility": "MEMBERS"
                                }
                                """))
                .andExpect(status().isCreated())
                .andDo(document("calendar-events/create",
                        requestFields(
                                fieldWithPath("title").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("description").type(JsonFieldType.STRING).description("설명").optional(),
                                fieldWithPath("category").type(JsonFieldType.STRING).description("카테고리"),
                                fieldWithPath("startAt").type(JsonFieldType.STRING).description("시작 시각"),
                                fieldWithPath("endAt").type(JsonFieldType.STRING).description("종료 시각"),
                                fieldWithPath("location").type(JsonFieldType.STRING).description("장소").optional(),
                                fieldWithPath("allDay").type(JsonFieldType.BOOLEAN).description("종일 여부"),
                                fieldWithPath("visibility").type(JsonFieldType.STRING).description("공개 범위")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.eventId").type(JsonFieldType.NUMBER).description("일정 ID"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("data.description").type(JsonFieldType.STRING).description("설명").optional(),
                                fieldWithPath("data.category").type(JsonFieldType.STRING).description("카테고리"),
                                fieldWithPath("data.startAt").type(JsonFieldType.STRING).description("시작 시각"),
                                fieldWithPath("data.endAt").type(JsonFieldType.STRING).description("종료 시각"),
                                fieldWithPath("data.location").type(JsonFieldType.STRING).description("장소").optional(),
                                fieldWithPath("data.allDay").type(JsonFieldType.BOOLEAN).description("종일 여부"),
                                fieldWithPath("data.visibility").type(JsonFieldType.STRING).description("공개 범위")
                        )
                ));
    }

    @Test
    void 일정_수정() throws Exception {
        when(calendarEventService.updateEvent(anyLong(), anyLong(), any(CalendarEventUpdateRequest.class))).thenReturn(createResponse());

        mockMvc.perform(put("/api/v1/calendar/events/{eventId}", 1L)
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "캡스톤 회의",
                                  "description": "주간 진행 상황 점검",
                                  "category": "GROUP",
                                  "startAt": "2026-03-21T14:00:00",
                                  "endAt": "2026-03-21T15:30:00",
                                  "location": "공학관 301호",
                                  "allDay": false,
                                  "visibility": "MEMBERS"
                                }
                                """))
                .andExpect(status().isOk())
                .andDo(document("calendar-events/update",
                        pathParameters(
                                parameterWithName("eventId").description("수정할 일정 ID")
                        ),
                        requestFields(
                                fieldWithPath("title").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("description").type(JsonFieldType.STRING).description("설명").optional(),
                                fieldWithPath("category").type(JsonFieldType.STRING).description("카테고리"),
                                fieldWithPath("startAt").type(JsonFieldType.STRING).description("시작 시각"),
                                fieldWithPath("endAt").type(JsonFieldType.STRING).description("종료 시각"),
                                fieldWithPath("location").type(JsonFieldType.STRING).description("장소").optional(),
                                fieldWithPath("allDay").type(JsonFieldType.BOOLEAN).description("종일 여부"),
                                fieldWithPath("visibility").type(JsonFieldType.STRING).description("공개 범위")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.eventId").type(JsonFieldType.NUMBER).description("일정 ID"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("data.description").type(JsonFieldType.STRING).description("설명").optional(),
                                fieldWithPath("data.category").type(JsonFieldType.STRING).description("카테고리"),
                                fieldWithPath("data.startAt").type(JsonFieldType.STRING).description("시작 시각"),
                                fieldWithPath("data.endAt").type(JsonFieldType.STRING).description("종료 시각"),
                                fieldWithPath("data.location").type(JsonFieldType.STRING).description("장소").optional(),
                                fieldWithPath("data.allDay").type(JsonFieldType.BOOLEAN).description("종일 여부"),
                                fieldWithPath("data.visibility").type(JsonFieldType.STRING).description("공개 범위")
                        )
                ));
    }

    @Test
    void 일정_삭제() throws Exception {
        doNothing().when(calendarEventService).deleteEvent(1L, 1L);

        mockMvc.perform(delete("/api/v1/calendar/events/{eventId}", 1L)
                        .session(createSession()))
                .andExpect(status().isOk())
                .andDo(document("calendar-events/delete",
                        pathParameters(
                                parameterWithName("eventId").description("삭제할 일정 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("데이터 없음")
                        )
                ));
    }

    @Test
    void 다가오는_일정_조회() throws Exception {
        when(calendarEventQueryService.getUpcomingEvents(1L, 5)).thenReturn(List.of(createResponse()));

        mockMvc.perform(get("/api/v1/calendar/events/upcoming")
                        .session(createSession())
                        .queryParam("size", "5"))
                .andExpect(status().isOk())
                .andDo(document("calendar-events/get-upcoming",
                        queryParameters(
                                parameterWithName("size").description("조회 개수").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data[].eventId").type(JsonFieldType.NUMBER).description("일정 ID"),
                                fieldWithPath("data[].title").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("data[].description").type(JsonFieldType.STRING).description("설명").optional(),
                                fieldWithPath("data[].category").type(JsonFieldType.STRING).description("카테고리"),
                                fieldWithPath("data[].startAt").type(JsonFieldType.STRING).description("시작 시각"),
                                fieldWithPath("data[].endAt").type(JsonFieldType.STRING).description("종료 시각"),
                                fieldWithPath("data[].location").type(JsonFieldType.STRING).description("장소").optional(),
                                fieldWithPath("data[].allDay").type(JsonFieldType.BOOLEAN).description("종일 여부"),
                                fieldWithPath("data[].visibility").type(JsonFieldType.STRING).description("공개 범위")
                        )
                ));
    }

    private MockHttpSession createSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }

    private CalendarEventResponse createResponse() {
        return new CalendarEventResponse(
                1L,
                "캡스톤 회의",
                "주간 진행 상황 점검",
                CalendarEventCategory.GROUP,
                LocalDateTime.of(2026, 3, 21, 14, 0),
                LocalDateTime.of(2026, 3, 21, 15, 30),
                "공학관 301호",
                false,
                CalendarEventVisibility.MEMBERS
        );
    }
}
