package cluverse.major.controller;

import cluverse.docs.RestDocsSupport;
import cluverse.major.service.MajorService;
import cluverse.major.service.response.MajorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MajorControllerDocsTest extends RestDocsSupport {

    private final MajorService majorService = mock(MajorService.class);

    @Override
    protected Object initController() {
        return new MajorController(majorService);
    }

    @Test
    void 전공_목록_조회() throws Exception {
        // given
        when(majorService.getMajors(1L)).thenReturn(List.of(
                new MajorResponse(10L, 210L, "컴퓨터공학", 1L, 1, 1),
                new MajorResponse(11L, 211L, "인공지능", 1L, 1, 2)
        ));

        // when, then
        mockMvc.perform(get("/api/v1/majors")
                        .queryParam("parentMajorId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].majorId").value(10))
                .andDo(document("majors/get-list",
                        queryParameters(
                                parameterWithName("parentMajorId").description("하위 전공을 조회할 부모 전공 ID").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("전공 목록"),
                                fieldWithPath("data[].majorId").type(JsonFieldType.NUMBER).description("전공 ID"),
                                fieldWithPath("data[].boardId").type(JsonFieldType.NUMBER).description("연결된 보드 ID"),
                                fieldWithPath("data[].name").type(JsonFieldType.STRING).description("전공명"),
                                fieldWithPath("data[].parentId").type(JsonFieldType.NUMBER).description("상위 전공 ID").optional(),
                                fieldWithPath("data[].depth").type(JsonFieldType.NUMBER).description("전공 depth"),
                                fieldWithPath("data[].displayOrder").type(JsonFieldType.NUMBER).description("노출 순서")
                        )
                ));
    }
}
