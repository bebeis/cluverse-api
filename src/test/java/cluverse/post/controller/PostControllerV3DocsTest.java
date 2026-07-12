package cluverse.post.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import cluverse.post.domain.PostCategory;
import cluverse.post.service.PostQueryService;
import cluverse.post.service.response.PostAuthorResponse;
import cluverse.post.service.response.PostPageResponse;
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

class PostControllerV3DocsTest extends RestDocsSupport {

    private final PostQueryService postQueryService = mock(PostQueryService.class);

    @Override
    protected Object initController() {
        return new PostControllerV3(postQueryService);
    }

    @Test
    void 게시글_목록_조회_V3() throws Exception {
        when(postQueryService.getPosts(anyLong(), any())).thenReturn(new PostPageResponse(
                List.of(createPostSummaryResponse(LocalDateTime.of(2026, 3, 13, 10, 0))),
                1,
                20,
                true,
                10,
                true,
                false
        ));

        mockMvc.perform(get("/api/v3/posts")
                        .session(createSession())
                        .queryParam("boardId", "3")
                        .queryParam("sort", "LATEST")
                        .queryParam("page", "1")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts[0].postId").value(10))
                .andDo(document("posts/get-post-list-v3",
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
                                fieldWithPath("data.lastPage").type(JsonFieldType.NUMBER).description("현재 페이지 블록에서 렌더링할 마지막 페이지 번호. 날짜 기반 조회 시 null").optional(),
                                fieldWithPath("data.hasNextBlock").type(JsonFieldType.BOOLEAN).description("다음 페이지 블록 존재 여부. 날짜 기반 조회 시 null").optional(),
                                fieldWithPath("data.dateBased").type(JsonFieldType.BOOLEAN).description("날짜 기반 조회 여부")
                        )
                ));
    }

    @Test
    void 날짜_기반_게시글_목록_조회() throws Exception {
        when(postQueryService.getPosts(anyLong(), any())).thenReturn(new PostPageResponse(
                List.of(createPostSummaryResponse(LocalDateTime.of(2024, 1, 15, 10, 0))),
                null,
                20,
                false,
                true
        ));

        mockMvc.perform(get("/api/v3/posts")
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
                                fieldWithPath("data.lastPage").type(JsonFieldType.NULL).description("마지막 페이지 번호 (날짜 기반 조회 시 null)"),
                                fieldWithPath("data.hasNextBlock").type(JsonFieldType.NULL).description("다음 페이지 블록 존재 여부 (날짜 기반 조회 시 null)"),
                                fieldWithPath("data.dateBased").type(JsonFieldType.BOOLEAN).description("날짜 기반 조회 여부 (true)")
                        )
                ));
    }

    private PostSummaryResponse createPostSummaryResponse(LocalDateTime createdAt) {
        return new PostSummaryResponse(
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
                createdAt
        );
    }

    private MockHttpSession createSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }
}
