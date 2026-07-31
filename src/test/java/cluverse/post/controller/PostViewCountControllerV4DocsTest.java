package cluverse.post.controller;

import cluverse.docs.RestDocsSupport;
import cluverse.post.service.PostViewCountServiceV4;
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

class PostViewCountControllerV4DocsTest extends RestDocsSupport {

    private final PostViewCountServiceV4 postViewCountService = mock(PostViewCountServiceV4.class);

    @Override
    protected Object initController() {
        return new PostViewCountControllerV4(postViewCountService);
    }

    @Test
    void 조회수_V4_증가() throws Exception {
        doNothing().when(postViewCountService).increaseViewCount(10L);

        mockMvc.perform(post("/api/v4/posts/{postId}/view-count", 10L))
                .andExpect(status().isOk())
                .andDo(document("posts/increase-view-count-v4",
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
