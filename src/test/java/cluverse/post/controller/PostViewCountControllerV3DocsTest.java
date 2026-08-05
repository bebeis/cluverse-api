package cluverse.post.controller;

import cluverse.docs.RestDocsSupport;
import cluverse.meta.service.implement.ViewCountSource;
import cluverse.post.service.PostViewCountServiceV3;
import cluverse.post.service.response.PostViewCountResponse;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostViewCountControllerV3DocsTest extends RestDocsSupport {

    private final PostViewCountServiceV3 postViewCountService = mock(PostViewCountServiceV3.class);
    private final ViewCountCookieResolver viewCountCookieResolver = new ViewCountCookieResolver();

    @Override
    protected Object initController() {
        return new PostViewCountControllerV3(postViewCountService, viewCountCookieResolver);
    }

    @Test
    void 기준치_기반_증분_조회수를_증가시킨다() throws Exception {
        when(postViewCountService.increaseViewCount(10L, "viewer-1"))
                .thenReturn(new PostViewCountResponse(10L, 8_001L, true, ViewCountSource.REDIS_DELTA));

        mockMvc.perform(post("/api/v3/posts/{postId}/view-count", 10L)
                        .cookie(new Cookie(ViewCountCookieResolver.COOKIE_NAME, "viewer-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(8_001L))
                .andDo(document("posts/increase-view-count-v3",
                        pathParameters(parameterWithName("postId").description("조회한 게시글 ID")),
                        responseFields(viewCountResponseFields())
                ));
    }

    private FieldDescriptor[] viewCountResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                fieldWithPath("data.postId").type(JsonFieldType.NUMBER).description("게시글 ID"),
                fieldWithPath("data.viewCount").type(JsonFieldType.NUMBER).description("현재 표시 조회수"),
                fieldWithPath("data.counted").type(JsonFieldType.BOOLEAN).description("이번 요청의 증가 반영 여부"),
                fieldWithPath("data.source").type(JsonFieldType.STRING).description("조회수 집계 저장소")
        };
    }
}
