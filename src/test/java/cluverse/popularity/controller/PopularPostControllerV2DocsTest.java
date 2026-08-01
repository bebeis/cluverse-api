package cluverse.popularity.controller;

import cluverse.docs.RestDocsSupport;
import cluverse.popularity.domain.PopularPostSortType;
import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.service.PopularPostQueryService;
import cluverse.popularity.service.response.PopularPostListResponse;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PopularPostControllerV2DocsTest extends RestDocsSupport {

    private final PopularPostQueryService popularPostQueryService = mock(PopularPostQueryService.class);

    @Override
    protected Object initController() {
        return new PopularPostControllerV2(popularPostQueryService);
    }

    @Test
    void 과거_인기글_V2_조회() throws Exception {
        when(popularPostQueryService.getHistory(PopularityAlgorithmVersion.V2, PopularPostSortType.SCORE, 20))
                .thenReturn(new PopularPostListResponse(
                        PopularityAlgorithmVersion.V2,
                        PopularPostSortType.SCORE,
                        List.of()
                ));

        mockMvc.perform(get("/api/v2/popular-posts/history")
                        .param("sort", "SCORE")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andDo(document("popular-posts/get-history-v2",
                        queryParameters(
                                parameterWithName("sort").description("LATEST 또는 SCORE"),
                                parameterWithName("size").description("조회 개수(1~100)")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.version").type(JsonFieldType.STRING).description("실행 구조 식별자"),
                                fieldWithPath("data.sort").type(JsonFieldType.STRING).description("정렬 방식"),
                                fieldWithPath("data.posts").type(JsonFieldType.ARRAY).description("인기글 목록")
                        )
                ));
    }
}
