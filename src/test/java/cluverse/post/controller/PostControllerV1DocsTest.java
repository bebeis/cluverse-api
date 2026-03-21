package cluverse.post.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import cluverse.post.domain.PostCategory;
import cluverse.post.service.PostService;
import cluverse.post.service.response.PostAuthorResponse;
import cluverse.post.service.response.PostBoardResponse;
import cluverse.post.service.response.PostDetailResponse;
import cluverse.post.service.response.PostPageResponse;
import cluverse.post.service.response.PostSummaryResponse;
import cluverse.post.service.response.PostTitleResponse;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static cluverse.common.auth.LoginMemberArgumentResolver.SESSION_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostControllerV1DocsTest extends RestDocsSupport {

    private final PostService postService = mock(PostService.class);

    @Override
    protected Object initController() {
        return new PostControllerV1(postService);
    }

    @Test
    void 게시글_목록_조회() throws Exception {
        when(postService.getPosts(anyLong(), any())).thenReturn(new PostPageResponse(
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
                                LocalDateTime.of(2026, 3, 13, 10, 0)
                        )
                ),
                1,
                20,
                true,
                false
        ));

        mockMvc.perform(get("/api/v1/posts")
                        .session(createSession())
                        .queryParam("boardId", "3")
                        .queryParam("sort", "LATEST")
                        .queryParam("page", "1")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts[0].postId").value(10))
                .andDo(document("posts/get-post-list",
                        queryParameters(
                                parameterWithName("boardId").description("조회할 게시판 ID"),
                                parameterWithName("category").description("게시글 카테고리").optional(),
                                parameterWithName("sort").description("정렬 기준 (`LATEST`, `VIEW_COUNT`). 날짜 기반 조회 시 생략").optional(),
                                parameterWithName("page").description("페이지 번호 (1~500). `date`와 함께 사용 불가").optional(),
                                parameterWithName("size").description("페이지 크기").optional(),
                                parameterWithName("date").description("날짜 기반 조회 (`yyyy-MM-dd`). 지정 시 해당 날짜의 글만 조회. `page`와 함께 사용 불가").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.posts").type(JsonFieldType.ARRAY).description("게시글 목록"),
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
                                fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지. 날짜 기반 조회 시 null").optional(),
                                fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),
                                fieldWithPath("data.dateBased").type(JsonFieldType.BOOLEAN).description("날짜 기반 조회 여부")
                        )
                ));
    }

    @Test
    void 게시글_검색() throws Exception {
        when(postService.searchPosts(anyLong(), any())).thenReturn(new PostPageResponse(
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
                                LocalDateTime.of(2026, 3, 13, 10, 0)
                        )
                ),
                1,
                20,
                true,
                false
        ));

        mockMvc.perform(get("/api/v1/posts/search")
                        .session(createSession())
                        .queryParam("boardId", "3")
                        .queryParam("keyword", "스프링")
                        .queryParam("page", "1")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts[0].postId").value(10))
                .andDo(document("posts/search-posts",
                        queryParameters(
                                parameterWithName("boardId").description("검색할 게시판 ID"),
                                parameterWithName("keyword").description("제목, 본문, 태그에서 검색할 키워드"),
                                parameterWithName("page").description("페이지 번호").optional(),
                                parameterWithName("size").description("페이지 크기").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.posts").type(JsonFieldType.ARRAY).description("검색된 게시글 목록"),
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
                                fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지"),
                                fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),
                                fieldWithPath("data.dateBased").type(JsonFieldType.BOOLEAN).description("날짜 기반 조회 여부 (false)")
                        )
                ));
    }

    @Test
    void 최근_댓글이_달린_게시글_조회() throws Exception {
        when(postService.getRecentCommentRepliedPosts(10L)).thenReturn(List.of(
                new PostTitleResponse(
                        10L,
                        "스프링 스터디 모집합니다",
                        LocalDateTime.of(2026, 3, 21, 15, 40)
                ),
                new PostTitleResponse(
                        7L,
                        "JPA 질문 있습니다",
                        LocalDateTime.of(2026, 3, 21, 14, 10)
                )
        ));

        mockMvc.perform(get("/api/v1/posts/recent-comment-replied")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].postId").value(10))
                .andDo(document("posts/get-recent-comment-replied-posts",
                        queryParameters(
                                parameterWithName("size").description("조회할 게시글 수").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("최근 댓글이 달린 게시글 목록"),
                                fieldWithPath("data[].postId").type(JsonFieldType.NUMBER).description("게시글 ID"),
                                fieldWithPath("data[].title").type(JsonFieldType.STRING).description("게시글 제목"),
                                fieldWithPath("data[].lastCommentRepliedAt").type(JsonFieldType.STRING).description("가장 최근 댓글 작성 시각")
                        )
                ));
    }

    @Test
    void 비회원도_게시글_목록을_조회할_수_있다() throws Exception {
        when(postService.getPosts(isNull(), any())).thenReturn(new PostPageResponse(
                List.of(),
                1,
                20,
                false,
                false
        ));

        mockMvc.perform(get("/api/v1/posts")
                        .queryParam("boardId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts").isArray());
    }

    @Test
    void 날짜_기반_게시글_목록_조회() throws Exception {
        // given
        when(postService.getPosts(anyLong(), any())).thenReturn(new PostPageResponse(
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
                                LocalDateTime.of(2024, 1, 15, 10, 0)
                        )
                ),
                null,
                20,
                false,
                true
        ));

        mockMvc.perform(get("/api/v1/posts")
                        .session(createSession())
                        .queryParam("boardId", "3")
                        .queryParam("date", "2024-01-15")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dateBased").value(true))
                .andExpect(jsonPath("$.data.page").doesNotExist())
                .andDo(document("posts/get-post-list-by-date",
                        queryParameters(
                                parameterWithName("boardId").description("조회할 게시판 ID"),
                                parameterWithName("date").description("조회 날짜 (`yyyy-MM-dd`). 해당 날짜 하루치 글만 반환"),
                                parameterWithName("size").description("페이지 크기").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.posts").type(JsonFieldType.ARRAY).description("게시글 목록"),
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
                                fieldWithPath("data.page").type(JsonFieldType.NULL).description("페이지 번호 (날짜 기반 조회 시 null)"),
                                fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),
                                fieldWithPath("data.dateBased").type(JsonFieldType.BOOLEAN).description("날짜 기반 조회 여부 (true)")
                        )
                ));
    }

    @Test
    void 게시글_작성() throws Exception {
        when(postService.createPost(anyLong(), any(), any())).thenReturn(createPostDetailResponse());

        mockMvc.perform(post("/api/v1/posts")
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "boardId": 3,
                                    "title": "스프링 스터디 모집합니다",
                                    "content": "주 1회 온라인으로 진행할 예정입니다.",
                                    "category": "INFORMATION",
                                    "tags": ["spring", "backend"],
                                    "isAnonymous": false,
                                    "isPinned": false,
                                    "isExternalVisible": true,
                                    "imageUrls": [
                                        "https://cdn.example.com/posts/image-1.png",
                                        "https://cdn.example.com/posts/image-2.png"
                                    ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.postId").value(10))
                .andDo(document("posts/create-post",
                        requestFields(
                                fieldWithPath("boardId").type(JsonFieldType.NUMBER).description("게시판 ID"),
                                fieldWithPath("title").type(JsonFieldType.STRING).description("게시글 제목"),
                                fieldWithPath("content").type(JsonFieldType.STRING).description("게시글 본문"),
                                fieldWithPath("category").type(JsonFieldType.STRING).description("게시글 카테고리"),
                                fieldWithPath("tags").type(JsonFieldType.ARRAY).description("태그 목록"),
                                fieldWithPath("isAnonymous").type(JsonFieldType.BOOLEAN).description("익명 여부"),
                                fieldWithPath("isPinned").type(JsonFieldType.BOOLEAN).description("상단 고정 여부"),
                                fieldWithPath("isExternalVisible").type(JsonFieldType.BOOLEAN).description("외부 공개 여부"),
                                fieldWithPath("imageUrls").type(JsonFieldType.ARRAY).description("첨부 이미지 URL 목록")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.postId").type(JsonFieldType.NUMBER).description("게시글 ID"),
                                fieldWithPath("data.boardId").type(JsonFieldType.NUMBER).description("게시판 ID"),
                                fieldWithPath("data.board.boardId").type(JsonFieldType.NUMBER).description("게시판 ID"),
                                fieldWithPath("data.board.boardType").type(JsonFieldType.STRING).description("게시판 타입"),
                                fieldWithPath("data.board.name").type(JsonFieldType.STRING).description("게시판 이름"),
                                fieldWithPath("data.board.parentBoardId").type(JsonFieldType.NULL).description("상위 게시판 ID").optional(),
                                fieldWithPath("data.category").type(JsonFieldType.STRING).description("게시글 카테고리"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING).description("게시글 제목"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING).description("게시글 본문"),
                                fieldWithPath("data.tags").type(JsonFieldType.ARRAY).description("태그 목록"),
                                fieldWithPath("data.imageUrls").type(JsonFieldType.ARRAY).description("첨부 이미지 URL 목록"),
                                fieldWithPath("data.isAnonymous").type(JsonFieldType.BOOLEAN).description("익명 여부"),
                                fieldWithPath("data.isPinned").type(JsonFieldType.BOOLEAN).description("상단 고정 여부"),
                                fieldWithPath("data.isExternalVisible").type(JsonFieldType.BOOLEAN).description("외부 공개 여부"),
                                fieldWithPath("data.viewCount").type(JsonFieldType.NUMBER).description("조회수"),
                                fieldWithPath("data.likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                                fieldWithPath("data.commentCount").type(JsonFieldType.NUMBER).description("댓글 수"),
                                fieldWithPath("data.bookmarkCount").type(JsonFieldType.NUMBER).description("북마크 수"),
                                fieldWithPath("data.author.memberId").type(JsonFieldType.NUMBER).description("작성자 회원 ID"),
                                fieldWithPath("data.author.nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                                fieldWithPath("data.author.profileImageUrl").type(JsonFieldType.STRING).description("작성자 프로필 이미지 URL").optional(),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("작성 시각"),
                                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정 시각")
                        )
                ));
    }

    @Test
    void 게시글_상세_조회() throws Exception {
        when(postService.readPost(1L, 10L)).thenReturn(createPostDetailResponse());

        mockMvc.perform(get("/api/v1/posts/{postId}", 10L)
                        .session(createSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(10))
                .andDo(document("posts/read-post",
                        pathParameters(
                                parameterWithName("postId").description("조회할 게시글 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.postId").type(JsonFieldType.NUMBER).description("게시글 ID"),
                                fieldWithPath("data.boardId").type(JsonFieldType.NUMBER).description("게시판 ID"),
                                fieldWithPath("data.board.boardId").type(JsonFieldType.NUMBER).description("게시판 ID"),
                                fieldWithPath("data.board.boardType").type(JsonFieldType.STRING).description("게시판 타입"),
                                fieldWithPath("data.board.name").type(JsonFieldType.STRING).description("게시판 이름"),
                                fieldWithPath("data.board.parentBoardId").type(JsonFieldType.NULL).description("상위 게시판 ID").optional(),
                                fieldWithPath("data.category").type(JsonFieldType.STRING).description("게시글 카테고리"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING).description("게시글 제목"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING).description("게시글 본문"),
                                fieldWithPath("data.tags").type(JsonFieldType.ARRAY).description("태그 목록"),
                                fieldWithPath("data.imageUrls").type(JsonFieldType.ARRAY).description("첨부 이미지 URL 목록"),
                                fieldWithPath("data.isAnonymous").type(JsonFieldType.BOOLEAN).description("익명 여부"),
                                fieldWithPath("data.isPinned").type(JsonFieldType.BOOLEAN).description("상단 고정 여부"),
                                fieldWithPath("data.isExternalVisible").type(JsonFieldType.BOOLEAN).description("외부 공개 여부"),
                                fieldWithPath("data.viewCount").type(JsonFieldType.NUMBER).description("조회수"),
                                fieldWithPath("data.likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                                fieldWithPath("data.commentCount").type(JsonFieldType.NUMBER).description("댓글 수"),
                                fieldWithPath("data.bookmarkCount").type(JsonFieldType.NUMBER).description("북마크 수"),
                                fieldWithPath("data.author.memberId").type(JsonFieldType.NUMBER).description("작성자 회원 ID"),
                                fieldWithPath("data.author.nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                                fieldWithPath("data.author.profileImageUrl").type(JsonFieldType.STRING).description("작성자 프로필 이미지 URL").optional(),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("작성 시각"),
                                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정 시각")
                        )
                ));

        verify(postService).readPost(1L, 10L);
    }

    @Test
    void 게시글_수정() throws Exception {
        when(postService.updatePost(anyLong(), anyLong(), any())).thenReturn(createUpdatedPostDetailResponse());

        mockMvc.perform(put("/api/v1/posts/{postId}", 10L)
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "스프링 스터디 모집합니다 [수정]",
                                    "content": "오프라인 병행으로 진행합니다.",
                                    "category": "INFORMATION",
                                    "tags": ["spring", "backend", "study"],
                                    "isAnonymous": false,
                                    "isPinned": true,
                                    "isExternalVisible": true,
                                    "imageUrls": [
                                        "https://cdn.example.com/posts/image-1.png"
                                    ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("스프링 스터디 모집합니다 [수정]"))
                .andDo(document("posts/update-post",
                        pathParameters(
                                parameterWithName("postId").description("수정할 게시글 ID")
                        ),
                        requestFields(
                                fieldWithPath("title").type(JsonFieldType.STRING).description("게시글 제목"),
                                fieldWithPath("content").type(JsonFieldType.STRING).description("게시글 본문"),
                                fieldWithPath("category").type(JsonFieldType.STRING).description("게시글 카테고리"),
                                fieldWithPath("tags").type(JsonFieldType.ARRAY).description("태그 목록"),
                                fieldWithPath("isAnonymous").type(JsonFieldType.BOOLEAN).description("익명 여부"),
                                fieldWithPath("isPinned").type(JsonFieldType.BOOLEAN).description("상단 고정 여부"),
                                fieldWithPath("isExternalVisible").type(JsonFieldType.BOOLEAN).description("외부 공개 여부"),
                                fieldWithPath("imageUrls").type(JsonFieldType.ARRAY).description("첨부 이미지 URL 목록")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.postId").type(JsonFieldType.NUMBER).description("게시글 ID"),
                                fieldWithPath("data.boardId").type(JsonFieldType.NUMBER).description("게시판 ID"),
                                fieldWithPath("data.board.boardId").type(JsonFieldType.NUMBER).description("게시판 ID"),
                                fieldWithPath("data.board.boardType").type(JsonFieldType.STRING).description("게시판 타입"),
                                fieldWithPath("data.board.name").type(JsonFieldType.STRING).description("게시판 이름"),
                                fieldWithPath("data.board.parentBoardId").type(JsonFieldType.NULL).description("상위 게시판 ID").optional(),
                                fieldWithPath("data.category").type(JsonFieldType.STRING).description("게시글 카테고리"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING).description("게시글 제목"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING).description("게시글 본문"),
                                fieldWithPath("data.tags").type(JsonFieldType.ARRAY).description("태그 목록"),
                                fieldWithPath("data.imageUrls").type(JsonFieldType.ARRAY).description("첨부 이미지 URL 목록"),
                                fieldWithPath("data.isAnonymous").type(JsonFieldType.BOOLEAN).description("익명 여부"),
                                fieldWithPath("data.isPinned").type(JsonFieldType.BOOLEAN).description("상단 고정 여부"),
                                fieldWithPath("data.isExternalVisible").type(JsonFieldType.BOOLEAN).description("외부 공개 여부"),
                                fieldWithPath("data.viewCount").type(JsonFieldType.NUMBER).description("조회수"),
                                fieldWithPath("data.likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                                fieldWithPath("data.commentCount").type(JsonFieldType.NUMBER).description("댓글 수"),
                                fieldWithPath("data.bookmarkCount").type(JsonFieldType.NUMBER).description("북마크 수"),
                                fieldWithPath("data.author.memberId").type(JsonFieldType.NUMBER).description("작성자 회원 ID"),
                                fieldWithPath("data.author.nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                                fieldWithPath("data.author.profileImageUrl").type(JsonFieldType.STRING).description("작성자 프로필 이미지 URL").optional(),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("작성 시각"),
                                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정 시각")
                        )
                ));
    }

    @Test
    void 게시글_삭제() throws Exception {
        doNothing().when(postService).deletePost(1L, 10L);

        mockMvc.perform(delete("/api/v1/posts/{postId}", 10L)
                        .session(createSession()))
                .andExpect(status().isOk())
                .andDo(document("posts/delete-post",
                        pathParameters(
                                parameterWithName("postId").description("삭제할 게시글 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("데이터 없음")
                        )
                ));
    }

    private MockHttpSession createSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }

    private PostDetailResponse createPostDetailResponse() {
        return new PostDetailResponse(
                10L,
                3L,
                new PostBoardResponse(3L, cluverse.board.domain.BoardType.INTEREST, "AI", null),
                PostCategory.INFORMATION,
                "스프링 스터디 모집합니다",
                "주 1회 온라인으로 진행할 예정입니다.",
                List.of("spring", "backend"),
                List.of(
                        "https://cdn.example.com/posts/image-1.png",
                        "https://cdn.example.com/posts/image-2.png"
                ),
                false,
                false,
                true,
                120L,
                15L,
                4L,
                8L,
                new PostAuthorResponse(1L, "luna", "https://cdn.example.com/profile.png"),
                LocalDateTime.of(2026, 3, 13, 10, 0),
                LocalDateTime.of(2026, 3, 13, 10, 0)
        );
    }

    private PostDetailResponse createUpdatedPostDetailResponse() {
        return new PostDetailResponse(
                10L,
                3L,
                new PostBoardResponse(3L, cluverse.board.domain.BoardType.INTEREST, "AI", null),
                PostCategory.INFORMATION,
                "스프링 스터디 모집합니다 [수정]",
                "오프라인 병행으로 진행합니다.",
                List.of("spring", "backend", "study"),
                List.of("https://cdn.example.com/posts/image-1.png"),
                false,
                true,
                true,
                140L,
                18L,
                5L,
                9L,
                new PostAuthorResponse(1L, "luna", "https://cdn.example.com/profile.png"),
                LocalDateTime.of(2026, 3, 13, 10, 0),
                LocalDateTime.of(2026, 3, 13, 11, 30)
        );
    }
}
