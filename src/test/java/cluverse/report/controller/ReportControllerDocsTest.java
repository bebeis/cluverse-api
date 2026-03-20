package cluverse.report.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import cluverse.report.domain.ReportReasonCode;
import cluverse.report.domain.ReportStatus;
import cluverse.report.domain.ReportTargetType;
import cluverse.report.service.ReportService;
import cluverse.report.service.request.ReportCreateRequest;
import cluverse.report.service.response.ReportReasonResponse;
import cluverse.report.service.response.ReportResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static cluverse.common.auth.LoginMemberArgumentResolver.SESSION_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportControllerDocsTest extends RestDocsSupport {

    private final ReportService reportService = mock(ReportService.class);

    @Override
    protected Object initController() {
        return new ReportController(reportService);
    }

    @Test
    void 신고_생성() throws Exception {
        when(reportService.createReport(anyLong(), any(ReportCreateRequest.class))).thenReturn(new ReportResponse(
                1L,
                ReportTargetType.POST,
                10L,
                ReportReasonCode.SPAM,
                "반복 광고 게시물입니다.",
                List.of("https://cdn.example.com/report/image-1.png"),
                ReportStatus.RECEIVED
        ));

        mockMvc.perform(post("/api/v1/reports")
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "POST",
                                  "targetId": 10,
                                  "reasonCode": "SPAM",
                                  "detail": "반복 광고 게시물입니다.",
                                  "evidenceImageUrls": ["https://cdn.example.com/report/image-1.png"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andDo(document("reports/create",
                        requestFields(
                                fieldWithPath("targetType").type(JsonFieldType.STRING).description("신고 대상 타입"),
                                fieldWithPath("targetId").type(JsonFieldType.NUMBER).description("신고 대상 ID"),
                                fieldWithPath("reasonCode").type(JsonFieldType.STRING).description("신고 사유 코드"),
                                fieldWithPath("detail").type(JsonFieldType.STRING).description("상세 설명").optional(),
                                fieldWithPath("evidenceImageUrls").type(JsonFieldType.ARRAY).description("증빙 이미지 URL 목록").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.reportId").type(JsonFieldType.NUMBER).description("신고 ID"),
                                fieldWithPath("data.targetType").type(JsonFieldType.STRING).description("신고 대상 타입"),
                                fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("신고 대상 ID"),
                                fieldWithPath("data.reasonCode").type(JsonFieldType.STRING).description("신고 사유 코드"),
                                fieldWithPath("data.detail").type(JsonFieldType.STRING).description("상세 설명").optional(),
                                fieldWithPath("data.evidenceImageUrls").type(JsonFieldType.ARRAY).description("증빙 이미지 URL 목록"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("신고 상태")
                        )
                ));
    }

    @Test
    void 신고_사유_목록_조회() throws Exception {
        when(reportService.getReasonCodes()).thenReturn(List.of(
                new ReportReasonResponse("SPAM", "스팸/광고"),
                new ReportReasonResponse("ABUSE", "욕설/혐오 표현")
        ));

        mockMvc.perform(get("/api/v1/report-reasons"))
                .andExpect(status().isOk())
                .andDo(document("reports/get-reasons",
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data[].reasonCode").type(JsonFieldType.STRING).description("신고 사유 코드"),
                                fieldWithPath("data[].description").type(JsonFieldType.STRING).description("신고 사유 설명")
                        )
                ));
    }

    private MockHttpSession createSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }
}
