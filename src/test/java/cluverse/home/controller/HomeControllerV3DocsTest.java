package cluverse.home.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.home.service.HomeQueryService;
import cluverse.home.service.response.RecentCommentedPostResponse;
import cluverse.member.domain.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static cluverse.common.auth.LoginMemberArgumentResolver.SESSION_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HomeControllerV3DocsTest extends RestDocsSupport {

    private final HomeQueryService homeQueryService = mock(HomeQueryService.class);

    @Override
    protected Object initController() {
        return new HomeControllerV3(homeQueryService);
    }

    @Test
    void 최근_댓글_글_활동_투영_조회() throws Exception {
        when(homeQueryService.getRecentCommentedPostsV3(1L)).thenReturn(List.of(
                new RecentCommentedPostResponse(
                        20L, "최근 대화", LocalDateTime.of(2026, 8, 3, 12, 0)
                )
        ));

        mockMvc.perform(get("/api/v3/home/recent-commented-posts").session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].postId").value(20))
                .andDo(document("home/get-recent-commented-posts-v3",
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("최근 댓글이 달린 게시글"),
                                fieldWithPath("data[].postId").type(JsonFieldType.NUMBER).description("게시글 ID"),
                                fieldWithPath("data[].title").type(JsonFieldType.STRING).description("게시글 제목"),
                                fieldWithPath("data[].lastCommentedAt").type(JsonFieldType.STRING).description("마지막 댓글 시각")
                        )
                ));
    }

    private MockHttpSession session() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }
}
