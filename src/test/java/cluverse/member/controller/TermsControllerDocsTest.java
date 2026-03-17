package cluverse.member.controller;

import cluverse.docs.RestDocsSupport;
import cluverse.member.service.TermsService;
import cluverse.member.service.response.TermsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TermsControllerDocsTest extends RestDocsSupport {

    private final TermsService termsService = mock(TermsService.class);

    @Override
    protected Object initController() {
        return new TermsController(termsService);
    }

    @Test
    void 약관_목록_조회() throws Exception {
        // given
        when(termsService.getTerms()).thenReturn(List.of(
                new TermsResponse(1L, "SERVICE", "서비스 이용약관", "약관 내용", "1.0.0", true, LocalDateTime.of(2026, 3, 1, 0, 0)),
                new TermsResponse(2L, "PRIVACY", "개인정보 처리방침", "개인정보 처리방침 내용", "1.0.0", true, LocalDateTime.of(2026, 3, 1, 0, 0))
        ));

        // when, then
        mockMvc.perform(get("/api/v1/terms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].termsId").value(1))
                .andDo(document("terms/get-list",
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("약관 목록"),
                                fieldWithPath("data[].termsId").type(JsonFieldType.NUMBER).description("약관 ID"),
                                fieldWithPath("data[].termsType").type(JsonFieldType.STRING).description("약관 타입"),
                                fieldWithPath("data[].title").type(JsonFieldType.STRING).description("약관 제목"),
                                fieldWithPath("data[].content").type(JsonFieldType.STRING).description("약관 본문"),
                                fieldWithPath("data[].version").type(JsonFieldType.STRING).description("약관 버전"),
                                fieldWithPath("data[].required").type(JsonFieldType.BOOLEAN).description("필수 약관 여부"),
                                fieldWithPath("data[].effectiveAt").type(JsonFieldType.STRING).description("시행 일시")
                        )
                ));
    }
}
