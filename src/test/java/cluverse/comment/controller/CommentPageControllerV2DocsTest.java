package cluverse.comment.controller;

import cluverse.comment.service.CommentQueryService;
import cluverse.comment.service.response.CommentPageResponse;
import cluverse.docs.RestDocsSupport;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
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

class CommentPageControllerV2DocsTest extends RestDocsSupport {

    private final CommentQueryService commentQueryService = mock(CommentQueryService.class);

    @Override
    protected Object initController() {
        return new CommentPageControllerV2(commentQueryService);
    }

    @Test
    void 저장된_경로로_댓글_목록을_조회한다() throws Exception {
        when(commentQueryService.getCommentsV2(isNull(), any())).thenReturn(new CommentPageResponse(
                List.of(),
                null,
                20,
                false
        ));

        mockMvc.perform(get("/api/v2/comments")
                        .queryParam("postId", "10")
                        .queryParam("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.comments").isArray())
                .andDo(document("comments-v2/get-comments",
                        queryParameters(
                                parameterWithName("postId").description("댓글을 조회할 게시글 ID"),
                                parameterWithName("parentCommentId").description("특정 댓글 가지 조회 기준").optional(),
                                parameterWithName("cursor").description("다음 페이지 조회 커서").optional(),
                                parameterWithName("limit").description("조회할 댓글 수").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.comments").type(JsonFieldType.ARRAY).description("댓글 목록"),
                                fieldWithPath("data.nextCursor").type(JsonFieldType.NULL).description("다음 페이지 조회 커서"),
                                fieldWithPath("data.limit").type(JsonFieldType.NUMBER).description("요청 limit"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }
}
