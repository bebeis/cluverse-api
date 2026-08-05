package cluverse.post.controller;

import cluverse.docs.RestDocsSupport;
import cluverse.meta.service.implement.ViewCountSource;
import cluverse.post.service.PostViewCountServiceV4;
import cluverse.post.service.response.PostViewCountResponse;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostViewCountControllerV4DocsTest extends RestDocsSupport {

    private final PostViewCountServiceV4 postViewCountService = mock(PostViewCountServiceV4.class);
    private final ViewCountCookieResolver viewCountCookieResolver = new ViewCountCookieResolver();

    @Override
    protected Object initController() {
        return new PostViewCountControllerV4(postViewCountService, viewCountCookieResolver);
    }

    @Test
    void 전체_조회수_카운터를_증가시킨다() throws Exception {
        when(postViewCountService.increaseViewCount(10L, "viewer-1"))
                .thenReturn(new PostViewCountResponse(10L, 8_001L, true, ViewCountSource.REDIS_TOTAL));

        mockMvc.perform(post("/api/v4/posts/{postId}/view-count", 10L)
                        .cookie(new Cookie(ViewCountCookieResolver.COOKIE_NAME, "viewer-1")))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.data.viewCount").value(8_001L))
                .andExpect(jsonPath("$.data.counted").value(true))
                .andDo(document("posts/increase-view-count-v4",
                        pathParameters(
                                parameterWithName("postId").description("조회한 게시글 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.postId").type(JsonFieldType.NUMBER).description("게시글 ID"),
                                fieldWithPath("data.viewCount").type(JsonFieldType.NUMBER).description("현재 전체 조회수"),
                                fieldWithPath("data.counted").type(JsonFieldType.BOOLEAN).description("이번 요청의 증가 반영 여부"),
                                fieldWithPath("data.source").type(JsonFieldType.STRING).description("조회수 집계 저장소")
                        )
                ));
    }
}
