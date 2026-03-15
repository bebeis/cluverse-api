package cluverse.reaction.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import cluverse.reaction.service.CommentReactionService;
import cluverse.reaction.service.response.CommentLikeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.JsonFieldType;

import static cluverse.common.auth.LoginMemberArgumentResolver.SESSION_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentReactionControllerDocsTest extends RestDocsSupport {

    private final CommentReactionService commentReactionService = mock(CommentReactionService.class);

    @Override
    protected Object initController() {
        return new CommentReactionController(commentReactionService);
    }

    @Test
    void 댓글_좋아요() throws Exception {
        when(commentReactionService.likeComment(1L, 101L)).thenReturn(CommentLikeResponse.like(10L, 101L));

        mockMvc.perform(post("/api/v1/comments/{commentId}/likes", 101L)
                        .session(createSession()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andDo(document("comment-reactions/like-comment",
                        pathParameters(
                                parameterWithName("commentId").description("좋아요할 댓글 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.postId").type(JsonFieldType.NUMBER).description("댓글이 속한 게시글 ID"),
                                fieldWithPath("data.commentId").type(JsonFieldType.NUMBER).description("댓글 ID"),
                                fieldWithPath("data.liked").type(JsonFieldType.BOOLEAN).description("좋아요 상태 (`true`)")
                        )
                ));
    }

    @Test
    void 댓글_좋아요_취소() throws Exception {
        when(commentReactionService.unlikeComment(1L, 101L)).thenReturn(CommentLikeResponse.unlike(10L, 101L));

        mockMvc.perform(delete("/api/v1/comments/{commentId}/likes", 101L)
                        .session(createSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false))
                .andDo(document("comment-reactions/unlike-comment",
                        pathParameters(
                                parameterWithName("commentId").description("좋아요를 취소할 댓글 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.postId").type(JsonFieldType.NUMBER).description("댓글이 속한 게시글 ID"),
                                fieldWithPath("data.commentId").type(JsonFieldType.NUMBER).description("댓글 ID"),
                                fieldWithPath("data.liked").type(JsonFieldType.BOOLEAN).description("좋아요 상태 (`false`)")
                        )
                ));
    }

    private MockHttpSession createSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }
}
