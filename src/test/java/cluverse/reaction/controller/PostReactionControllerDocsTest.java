package cluverse.reaction.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import cluverse.reaction.service.PostReactionService;
import cluverse.reaction.service.request.BookmarkedPostSortType;
import cluverse.reaction.service.response.BookmarkedPostPageResponse;
import cluverse.reaction.service.response.PostBookmarkResponse;
import cluverse.reaction.service.response.PostLikeResponse;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cluverse.feed.service.response.FeedAuthorResponse;
import cluverse.feed.service.response.FeedBoardResponse;
import cluverse.feed.service.response.FeedPostSummaryResponse;
import cluverse.post.domain.PostCategory;
import static org.mockito.ArgumentMatchers.any;

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

    @Test
    void 북마크_목록_조회() throws Exception {
        when(postReactionService.getBookmarkedPosts(any(), any())).thenReturn(new BookmarkedPostPageResponse(
                List.of(
                        new FeedPostSummaryResponse(
                                10L,
                                new FeedBoardResponse(3L, cluverse.board.domain.BoardType.INTEREST, "AI", null),
                                PostCategory.INFORMATION,
                                "스프링 스터디 모집",
                                "본문 미리보기",
                                List.of("spring"),
                                "https://cdn.example.com/thumb.png",
                                false,
                                false,
                                true,
                                false,
                                false,
                                true,
                                false,
                                12L,
                                4L,
                                1L,
                                2L,
                                new FeedAuthorResponse(2L, "luna", "https://cdn.example.com/profile.png"),
                                LocalDateTime.of(2026, 3, 20, 12, 0)
                        )
                ),
                1,
                20,
                true,
                BookmarkedPostSortType.BOOKMARKED_AT
        ));

        mockMvc.perform(get("/api/v1/posts/bookmarks")
                        .session(createSession())
                        .queryParam("page", "1")
                        .queryParam("size", "20")
                        .queryParam("sort", "BOOKMARKED_AT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts[0].postId").value(10))
                .andExpect(jsonPath("$.data.posts[0].bookmarked").value(true))
                .andDo(document("post-reactions/get-bookmarked-posts",
                        queryParameters(
                                parameterWithName("sort").description("정렬 기준 (`BOOKMARKED_AT`, `LATEST`)").optional(),
                                parameterWithName("page").description("페이지 번호").optional(),
                                parameterWithName("size").description("페이지 크기").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.posts").type(JsonFieldType.ARRAY).description("북마크한 게시글 목록"),
                                fieldWithPath("data.posts[].postId").type(JsonFieldType.NUMBER).description("게시글 ID"),
                                fieldWithPath("data.posts[].board.boardId").type(JsonFieldType.NUMBER).description("게시판 ID"),
                                fieldWithPath("data.posts[].board.boardType").type(JsonFieldType.STRING).description("게시판 타입"),
                                fieldWithPath("data.posts[].board.name").type(JsonFieldType.STRING).description("게시판 이름"),
                                fieldWithPath("data.posts[].board.parentBoardId").type(JsonFieldType.NULL).description("상위 게시판 ID").optional(),
                                fieldWithPath("data.posts[].category").type(JsonFieldType.STRING).description("게시글 카테고리"),
                                fieldWithPath("data.posts[].title").type(JsonFieldType.STRING).description("게시글 제목"),
                                fieldWithPath("data.posts[].contentPreview").type(JsonFieldType.STRING).description("게시글 본문 미리보기"),
                                fieldWithPath("data.posts[].tags").type(JsonFieldType.ARRAY).description("태그 목록"),
                                fieldWithPath("data.posts[].thumbnailImageUrl").type(JsonFieldType.STRING).description("썸네일 이미지 URL").optional(),
                                fieldWithPath("data.posts[].isAnonymous").type(JsonFieldType.BOOLEAN).description("익명 여부"),
                                fieldWithPath("data.posts[].isPinned").type(JsonFieldType.BOOLEAN).description("상단 고정 여부"),
                                fieldWithPath("data.posts[].isExternalVisible").type(JsonFieldType.BOOLEAN).description("외부 공개 여부"),
                                fieldWithPath("data.posts[].isMine").type(JsonFieldType.BOOLEAN).description("내 게시글 여부"),
                                fieldWithPath("data.posts[].liked").type(JsonFieldType.BOOLEAN).description("좋아요 여부"),
                                fieldWithPath("data.posts[].bookmarked").type(JsonFieldType.BOOLEAN).description("북마크 여부"),
                                fieldWithPath("data.posts[].hiddenByBlock").type(JsonFieldType.BOOLEAN).description("차단으로 숨김 여부"),
                                fieldWithPath("data.posts[].viewCount").type(JsonFieldType.NUMBER).description("조회수"),
                                fieldWithPath("data.posts[].likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                                fieldWithPath("data.posts[].commentCount").type(JsonFieldType.NUMBER).description("댓글 수"),
                                fieldWithPath("data.posts[].bookmarkCount").type(JsonFieldType.NUMBER).description("북마크 수"),
                                fieldWithPath("data.posts[].author.memberId").type(JsonFieldType.NUMBER).description("작성자 회원 ID"),
                                fieldWithPath("data.posts[].author.nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                                fieldWithPath("data.posts[].author.profileImageUrl").type(JsonFieldType.STRING).description("작성자 프로필 이미지 URL").optional(),
                                fieldWithPath("data.posts[].createdAt").type(JsonFieldType.STRING).description("작성 시각"),
                                fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지"),
                                fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),
                                fieldWithPath("data.sort").type(JsonFieldType.STRING).description("정렬 기준")
                        )
                ));
    }

    private MockHttpSession createSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }
}
