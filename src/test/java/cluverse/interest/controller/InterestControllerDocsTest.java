package cluverse.interest.controller;

import cluverse.docs.RestDocsSupport;
import cluverse.interest.service.InterestService;
import cluverse.interest.service.response.InterestResponse;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterestControllerDocsTest extends RestDocsSupport {

    private final InterestService interestService = mock(InterestService.class);

    @Override
    protected Object initController() {
        return new InterestController(interestService);
    }

    @Test
    void 관심사_목록_조회() throws Exception {
        // given
        when(interestService.getInterests()).thenReturn(List.of(
                new InterestResponse(1L, 101L, "인공지능", "TECH", 10L, 1),
                new InterestResponse(2L, 102L, "백엔드", "TECH", 1L, 2)
        ));

        // when, then
        mockMvc.perform(get("/api/v1/interests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].interestId").value(1))
                .andDo(document("interests/get-list",
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("관심사 목록"),
                                fieldWithPath("data[].interestId").type(JsonFieldType.NUMBER).description("관심사 ID"),
                                fieldWithPath("data[].boardId").type(JsonFieldType.NUMBER).description("연결된 보드 ID"),
                                fieldWithPath("data[].name").type(JsonFieldType.STRING).description("관심사명"),
                                fieldWithPath("data[].category").type(JsonFieldType.STRING).description("관심사 카테고리").optional(),
                                fieldWithPath("data[].parentId").type(JsonFieldType.NUMBER).description("상위 관심사 ID").optional(),
                                fieldWithPath("data[].displayOrder").type(JsonFieldType.NUMBER).description("노출 순서")
                        )
                ));
    }
}
