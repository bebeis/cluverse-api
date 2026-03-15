package cluverse.reaction.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import cluverse.reaction.service.PostReactionService;
import cluverse.reaction.service.response.PostBookmarkResponse;
import cluverse.reaction.service.response.PostLikeResponse;
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

class PostReactionControllerDocsTest extends RestDocsSupport {

    private final PostReactionService postReactionService = mock(PostReactionService.class);

    @Override
    protected Object initController() {
        return new PostReactionController(postReactionService);
    }

    @Test
    void 게시글_좋아요() throws Exception {
        when(postReactionService.likePost(1L, 10L)).thenReturn(PostLikeResponse.like(10L));

        mockMvc.perform(post("/api/v1/posts/{postId}/likes", 10L)
                        .session(createSession()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.postId").value(10))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andDo(document("post-reactions/like-post",
                        pathParameters(
                                parameterWithName("postId").description("좋아요할 게시글 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.postId").type(JsonFieldType.NUMBER).description("좋아요한 게시글 ID"),
                                fieldWithPath("data.liked").type(JsonFieldType.BOOLEAN).description("좋아요 상태 (`true`)")
                        )
                ));
    }

    @Test
    void 게시글_북마크() throws Exception {
        when(postReactionService.bookmarkPost(1L, 10L)).thenReturn(PostBookmarkResponse.bookmark(10L));

        mockMvc.perform(post("/api/v1/posts/{postId}/bookmarks", 10L)
                        .session(createSession()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.postId").value(10))
                .andExpect(jsonPath("$.data.bookmarked").value(true))
                .andDo(document("post-reactions/bookmark-post",
                        pathParameters(
                                parameterWithName("postId").description("북마크할 게시글 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.postId").type(JsonFieldType.NUMBER).description("북마크한 게시글 ID"),
                                fieldWithPath("data.bookmarked").type(JsonFieldType.BOOLEAN).description("북마크 상태 (`true`)")
                        )
                ));
    }

    @Test
    void 게시글_북마크_취소() throws Exception {
        when(postReactionService.removeBookmark(1L, 10L)).thenReturn(PostBookmarkResponse.remove(10L));

        mockMvc.perform(delete("/api/v1/posts/{postId}/bookmarks", 10L)
                        .session(createSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(10))
                .andExpect(jsonPath("$.data.bookmarked").value(false))
                .andDo(document("post-reactions/remove-bookmark",
                        pathParameters(
                                parameterWithName("postId").description("북마크를 취소할 게시글 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.postId").type(JsonFieldType.NUMBER).description("북마크를 취소한 게시글 ID"),
                                fieldWithPath("data.bookmarked").type(JsonFieldType.BOOLEAN).description("북마크 상태 (`false`)")
                        )
                ));
    }

    private MockHttpSession createSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }
}
