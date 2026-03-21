package cluverse.post.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.feed.service.FeedQueryService;
import cluverse.feed.service.response.FeedAuthorResponse;
import cluverse.feed.service.response.FeedBoardResponse;
import cluverse.feed.service.response.FeedPageResponse;
import cluverse.feed.service.response.FeedPostSummaryResponse;
import cluverse.member.domain.MemberRole;
import cluverse.board.domain.BoardType;
import cluverse.post.domain.PostCategory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static cluverse.common.auth.LoginMemberArgumentResolver.SESSION_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

class TrendingPostControllerDocsTest extends RestDocsSupport {

    private final FeedQueryService feedQueryService = mock(FeedQueryService.class);

    @Override
    protected Object initController() {
        return new TrendingPostController(feedQueryService);
    }

    @Test
    void 트렌딩_게시글_조회() throws Exception {
        when(feedQueryService.getTrendingPosts(anyLong(), any())).thenReturn(createFeedPageResponse());

        mockMvc.perform(get("/api/v1/posts/trending")
                        .session(createSession())
                        .queryParam("range", "DAY_7")
                        .queryParam("category", "INFORMATION")
                        .queryParam("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts[0].liked").value(true))
                .andDo(document("posts/get-trending-posts",
                        queryParameters(
                                parameterWithName("range").description("트렌딩 집계 기간 (`DAY_1`, `DAY_7`, `DAY_30`)").optional(),
                                parameterWithName("category").description("게시글 카테고리 필터").optional(),
                                parameterWithName("cursor").description("다음 페이지 조회용 커서").optional(),
                                parameterWithName("limit").description("조회할 게시글 수").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.posts").type(JsonFieldType.ARRAY).description("트렌딩 게시글 목록"),
                                fieldWithPath("data.posts[].postId").type(JsonFieldType.NUMBER).description("게시글 ID"),
                                fieldWithPath("data.posts[].board.boardId").type(JsonFieldType.NUMBER).description("게시판 ID"),
                                fieldWithPath("data.posts[].board.boardType").type(JsonFieldType.STRING).description("게시판 타입"),
                                fieldWithPath("data.posts[].board.name").type(JsonFieldType.STRING).description("게시판 이름"),
                                fieldWithPath("data.posts[].board.parentBoardId").type(JsonFieldType.NULL).description("상위 게시판 ID").optional(),
                                fieldWithPath("data.posts[].category").type(JsonFieldType.STRING).description("게시글 카테고리"),
                                fieldWithPath("data.posts[].title").type(JsonFieldType.STRING).description("게시글 제목"),
                                fieldWithPath("data.posts[].contentPreview").type(JsonFieldType.STRING).description("본문 미리보기"),
                                fieldWithPath("data.posts[].tags").type(JsonFieldType.ARRAY).description("태그 목록"),
                                fieldWithPath("data.posts[].thumbnailImageUrl").type(JsonFieldType.STRING).description("썸네일 이미지 URL").optional(),
                                fieldWithPath("data.posts[].isAnonymous").type(JsonFieldType.BOOLEAN).description("익명 여부"),
                                fieldWithPath("data.posts[].isPinned").type(JsonFieldType.BOOLEAN).description("상단 고정 여부"),
                                fieldWithPath("data.posts[].isExternalVisible").type(JsonFieldType.BOOLEAN).description("외부 공개 여부"),
                                fieldWithPath("data.posts[].isMine").type(JsonFieldType.BOOLEAN).description("내 게시글 여부"),
                                fieldWithPath("data.posts[].liked").type(JsonFieldType.BOOLEAN).description("내가 좋아요한 게시글 여부"),
                                fieldWithPath("data.posts[].bookmarked").type(JsonFieldType.BOOLEAN).description("내가 북마크한 게시글 여부"),
                                fieldWithPath("data.posts[].hiddenByBlock").type(JsonFieldType.BOOLEAN).description("차단에 의해 숨겨진 게시글 여부"),
                                fieldWithPath("data.posts[].viewCount").type(JsonFieldType.NUMBER).description("조회수"),
                                fieldWithPath("data.posts[].likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                                fieldWithPath("data.posts[].commentCount").type(JsonFieldType.NUMBER).description("댓글 수"),
                                fieldWithPath("data.posts[].bookmarkCount").type(JsonFieldType.NUMBER).description("북마크 수"),
                                fieldWithPath("data.posts[].author.memberId").type(JsonFieldType.NUMBER).description("작성자 회원 ID").optional(),
                                fieldWithPath("data.posts[].author.nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                                fieldWithPath("data.posts[].author.profileImageUrl").type(JsonFieldType.STRING).description("작성자 프로필 이미지 URL").optional(),
                                fieldWithPath("data.posts[].createdAt").type(JsonFieldType.STRING).description("작성 시각"),
                                fieldWithPath("data.nextCursor").type(JsonFieldType.STRING).description("다음 페이지 조회용 커서").optional(),
                                fieldWithPath("data.limit").type(JsonFieldType.NUMBER).description("요청 limit"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    void 비회원도_트렌딩_게시글을_조회할_수_있다() throws Exception {
        when(feedQueryService.getTrendingPosts(isNull(), any())).thenReturn(FeedPageResponse.empty(20));

        mockMvc.perform(get("/api/v1/posts/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts").isArray());
    }

    private FeedPageResponse createFeedPageResponse() {
        return new FeedPageResponse(
                List.of(
                        new FeedPostSummaryResponse(
                                10L,
                                new FeedBoardResponse(3L, BoardType.INTEREST, "AI", null),
                                PostCategory.INFORMATION,
                                "AI 스터디 모집",
                                "매주 온라인으로 진행합니다.",
                                List.of("ai", "study"),
                                "https://cdn.example.com/posts/10-thumb.png",
                                false,
                                false,
                                true,
                                false,
                                true,
                                false,
                                false,
                                120L,
                                15L,
                                7L,
                                4L,
                                new FeedAuthorResponse(2L, "luna", "https://cdn.example.com/profile.png"),
                                LocalDateTime.of(2026, 3, 16, 12, 0)
                        )
                ),
                "120|2026-03-16T12:00:00|10",
                20,
                true
        );
    }

    private MockHttpSession createSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }
}
