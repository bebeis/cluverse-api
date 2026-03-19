package cluverse.post.controller;

import cluverse.docs.RestDocsSupport;
import cluverse.post.service.PostViewCountServiceV1;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostViewCountControllerV1DocsTest extends RestDocsSupport {

    private final PostViewCountServiceV1 postViewCountService = mock(PostViewCountServiceV1.class);

    @Override
    protected Object initController() {
        return new PostViewCountControllerV1(postViewCountService);
    }

    @Test
    void 조회수_V1_증가() throws Exception {
        doNothing().when(postViewCountService).increaseViewCountV1(10L);

        mockMvc.perform(post("/api/v1/posts/{postId}/view-count/v1", 10L))
                .andExpect(status().isOk())
                .andDo(document("posts/increase-view-count-v1",
                        pathParameters(
                                parameterWithName("postId").description("조회수를 증가시킬 게시글 ID")
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
    void 조회수_V2_증가() throws Exception {
        doNothing().when(postViewCountService).increaseViewCountV2(10L);

        mockMvc.perform(post("/api/v1/posts/{postId}/view-count/v2", 10L))
                .andExpect(status().isOk())
                .andDo(document("posts/increase-view-count-v2",
                        pathParameters(
                                parameterWithName("postId").description("조회수를 증가시킬 게시글 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("데이터 없음")
                        )
                ));
    }
}
