package cluverse.post.controller;

import cluverse.docs.RestDocsSupport;
import cluverse.meta.service.implement.ViewCountSource;
import cluverse.post.service.PostViewCountServiceV1;
import cluverse.post.service.response.PostViewCountResponse;
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

class PostViewCountControllerV1DocsTest extends RestDocsSupport {

    private final PostViewCountServiceV1 postViewCountService = mock(PostViewCountServiceV1.class);

    @Override
    protected Object initController() {
        return new PostViewCountControllerV1(postViewCountService);
    }

    @Test
    void MySQL에서_조회수를_증가시킨다() throws Exception {
        when(postViewCountService.increaseViewCount(10L))
                .thenReturn(new PostViewCountResponse(10L, 8_001L, true, ViewCountSource.MYSQL));

        mockMvc.perform(post("/api/v1/posts/{postId}/view-count", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(8_001L))
                .andDo(document("posts/increase-view-count-v1",
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
