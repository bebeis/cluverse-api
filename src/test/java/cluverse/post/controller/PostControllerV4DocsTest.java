package cluverse.post.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import cluverse.post.domain.PostCategory;
import cluverse.post.service.PostListQueryServiceV4;
import cluverse.post.service.response.PostAuthorResponse;
import cluverse.post.service.response.PostCursorPageResponse;
import cluverse.post.service.response.PostCursorResponse;
import cluverse.post.service.response.PostSummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static cluverse.common.auth.LoginMemberArgumentResolver.SESSION_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

class PostControllerV4DocsTest extends RestDocsSupport {

    private final PostListQueryServiceV4 postListQueryServiceV4 = mock(PostListQueryServiceV4.class);

    @Override
    protected Object initController() {
        return new PostControllerV4(postListQueryServiceV4);
    }

    @Test
    void 게시글_목록_조회_V4_커서() throws Exception {
        when(postListQueryServiceV4.getPosts(anyLong(), any())).thenReturn(new PostCursorPageResponse(
                List.of(
                        new PostSummaryResponse(
                                10L,
                                3L,
                                PostCategory.INFORMATION,
                                "스프링 스터디 모집합니다",
                                "주 1회 온라인으로 진행할 예정입니다.",
                                List.of("spring", "backend"),
                                "https://cdn.example.com/posts/10-thumb.png",
                                false,
                                false,
                                true,
                                120L,
                                15L,
                                4L,
                                8L,
                                new PostAuthorResponse(2L, "luna", "https://cdn.example.com/profile.png"),
                                LocalDateTime.of(2026, 1, 20, 10, 0)
                        )
                ),
                20,
                true,
                true,
                new PostCursorResponse(LocalDateTime.of(2026, 1, 20, 10, 0), 10L),
                new PostCursorResponse(LocalDateTime.of(2026, 1, 19, 22, 30), 7L)
        ));

        mockMvc.perform(get("/api/v4/posts")
                        .session(createSession())
                        .queryParam("boardId", "3")
                        .queryParam("size", "20")
                        .queryParam("cursorCreatedAt", "2026-01-20T10:00:00")
                        .queryParam("cursorPostId", "11")
                        .queryParam("direction", "NEXT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts[0].postId").value(10))
                .andExpect(jsonPath("$.data.nextCursor.postId").value(7))
                .andDo(document("posts/get-post-list-v4",
                        queryParameters(
                                parameterWithName("boardId").description("조회할 게시판 ID"),
                                parameterWithName("category").description("게시글 카테고리").optional(),
                                parameterWithName("size").description("페이지 크기").optional(),
                                parameterWithName("date").description("진입 앵커 날짜 (`yyyy-MM-dd`). 해당 날짜 이하 최신순으로 진입. 커서와 함께 사용 불가").optional(),
                                parameterWithName("cursorCreatedAt").description("커서 작성 시각 (ISO-8601). 응답의 prevCursor/nextCursor.createdAt을 그대로 전달. cursorPostId와 함께 필수").optional(),
                                parameterWithName("cursorPostId").description("커서 게시글 ID. 응답의 prevCursor/nextCursor.postId를 그대로 전달").optional(),
                                parameterWithName("direction").description("이동 방향 (`NEXT`=과거로, `PREV`=최신으로). 커서와 함께만 사용, 기본 NEXT").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.posts").type(JsonFieldType.ARRAY).description("게시글 목록 (최신순)"),
                                fieldWithPath("data.posts[].postId").type(JsonFieldType.NUMBER).description("게시글 ID"),
                                fieldWithPath("data.posts[].boardId").type(JsonFieldType.NUMBER).description("게시판 ID"),
                                fieldWithPath("data.posts[].category").type(JsonFieldType.STRING).description("게시글 카테고리"),
                                fieldWithPath("data.posts[].title").type(JsonFieldType.STRING).description("게시글 제목"),
                                fieldWithPath("data.posts[].contentPreview").type(JsonFieldType.STRING).description("게시글 본문 미리보기"),
                                fieldWithPath("data.posts[].tags").type(JsonFieldType.ARRAY).description("태그 목록"),
                                fieldWithPath("data.posts[].thumbnailImageUrl").type(JsonFieldType.STRING).description("썸네일 이미지 URL").optional(),
                                fieldWithPath("data.posts[].isAnonymous").type(JsonFieldType.BOOLEAN).description("익명 여부"),
                                fieldWithPath("data.posts[].isPinned").type(JsonFieldType.BOOLEAN).description("상단 고정 여부"),
                                fieldWithPath("data.posts[].isExternalVisible").type(JsonFieldType.BOOLEAN).description("외부 공개 여부"),
                                fieldWithPath("data.posts[].viewCount").type(JsonFieldType.NUMBER).description("조회수"),
                                fieldWithPath("data.posts[].likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                                fieldWithPath("data.posts[].commentCount").type(JsonFieldType.NUMBER).description("댓글 수"),
                                fieldWithPath("data.posts[].bookmarkCount").type(JsonFieldType.NUMBER).description("북마크 수"),
                                fieldWithPath("data.posts[].author.memberId").type(JsonFieldType.NUMBER).description("작성자 회원 ID"),
                                fieldWithPath("data.posts[].author.nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                                fieldWithPath("data.posts[].author.profileImageUrl").type(JsonFieldType.STRING).description("작성자 프로필 이미지 URL").optional(),
                                fieldWithPath("data.posts[].createdAt").type(JsonFieldType.STRING).description("작성 시각"),
                                fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("더 과거 글 존재 여부"),
                                fieldWithPath("data.hasPrev").type(JsonFieldType.BOOLEAN).description("더 최신 글 존재 여부"),
                                fieldWithPath("data.prevCursor").type(JsonFieldType.OBJECT).description("이전(최신 방향) 페이지 커서 = 첫 게시글의 (createdAt, postId). 목록이 비면 null").optional(),
                                fieldWithPath("data.prevCursor.createdAt").type(JsonFieldType.STRING).description("커서 작성 시각").optional(),
                                fieldWithPath("data.prevCursor.postId").type(JsonFieldType.NUMBER).description("커서 게시글 ID").optional(),
                                fieldWithPath("data.nextCursor").type(JsonFieldType.OBJECT).description("다음(과거 방향) 페이지 커서 = 마지막 게시글의 (createdAt, postId). 목록이 비면 null").optional(),
                                fieldWithPath("data.nextCursor.createdAt").type(JsonFieldType.STRING).description("커서 작성 시각").optional(),
                                fieldWithPath("data.nextCursor.postId").type(JsonFieldType.NUMBER).description("커서 게시글 ID").optional()
                        )
                ));
    }

    @Test
    void 커서와_날짜를_함께_보내면_400() throws Exception {
        mockMvc.perform(get("/api/v4/posts")
                        .queryParam("boardId", "3")
                        .queryParam("date", "2026-01-20")
                        .queryParam("cursorCreatedAt", "2026-01-20T10:00:00")
                        .queryParam("cursorPostId", "11"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 커서_필드를_하나만_보내면_400() throws Exception {
        mockMvc.perform(get("/api/v4/posts")
                        .queryParam("boardId", "3")
                        .queryParam("cursorPostId", "11"))
                .andExpect(status().isBadRequest());
    }

    private MockHttpSession createSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }
}
