package cluverse.comment.controller;

import cluverse.comment.domain.CommentStatus;
import cluverse.comment.service.CommentService;
import cluverse.comment.service.CommentQueryService;
import cluverse.comment.service.response.CommentAuthorResponse;
import cluverse.comment.service.response.CommentDeleteResponse;
import cluverse.comment.service.response.CommentPageResponse;
import cluverse.comment.service.response.CommentResponse;
import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentControllerDocsTest extends RestDocsSupport {

    private final CommentQueryService commentQueryService = mock(CommentQueryService.class);
    private final CommentService commentService = mock(CommentService.class);

    @Override
    protected Object initController() {
        return new CommentController(commentQueryService, commentService);
    }

    @Test
    void 댓글_목록_조회() throws Exception {
        when(commentQueryService.getComments(anyLong(), any())).thenReturn(new CommentPageResponse(
                List.of(createCommentResponse(101L, null, 0, false, true)),
                0,
                20,
                true
        ));

        mockMvc.perform(get("/api/v1/comments")
                        .session(createSession())
                        .queryParam("postId", "10")
                        .queryParam("offset", "0")
                        .queryParam("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.comments[0].commentId").value(101))
                .andDo(document("comments/get-comments",
                        queryParameters(
                                parameterWithName("postId").description("댓글을 조회할 게시글 ID"),
                                parameterWithName("parentCommentId").description("특정 부모 댓글의 하위 댓글만 조회할 때 사용하는 부모 댓글 ID").optional(),
                                parameterWithName("offset").description("조회 시작 위치").optional(),
                                parameterWithName("limit").description("조회할 댓글 수").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.comments").type(JsonFieldType.ARRAY).description("댓글 목록"),
                                fieldWithPath("data.comments[].commentId").type(JsonFieldType.NUMBER).description("댓글 ID"),
                                fieldWithPath("data.comments[].postId").type(JsonFieldType.NUMBER).description("게시글 ID"),
                                fieldWithPath("data.comments[].parentCommentId").type(JsonFieldType.NULL).description("부모 댓글 ID. 최상위 댓글이면 null"),
                                fieldWithPath("data.comments[].depth").type(JsonFieldType.NUMBER).description("댓글 depth"),
                                fieldWithPath("data.comments[].content").type(JsonFieldType.STRING).description("댓글 내용"),
                                fieldWithPath("data.comments[].status").type(JsonFieldType.STRING).description("댓글 상태 (`ACTIVE`, `BLINDED`, `DELETED`)"),
                                fieldWithPath("data.comments[].isAnonymous").type(JsonFieldType.BOOLEAN).description("익명 여부"),
                                fieldWithPath("data.comments[].isMine").type(JsonFieldType.BOOLEAN).description("내 댓글 여부"),
                                fieldWithPath("data.comments[].likedByMe").type(JsonFieldType.BOOLEAN).description("내가 좋아요한 댓글인지 여부"),
                                fieldWithPath("data.comments[].blockedAuthor").type(JsonFieldType.BOOLEAN).description("차단한 작성자의 댓글인지 여부"),
                                fieldWithPath("data.comments[].likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                                fieldWithPath("data.comments[].replyCount").type(JsonFieldType.NUMBER).description("직계 대댓글 수"),
                                fieldWithPath("data.comments[].author.memberId").type(JsonFieldType.NUMBER).description("작성자 회원 ID").optional(),
                                fieldWithPath("data.comments[].author.nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                                fieldWithPath("data.comments[].author.profileImageUrl").type(JsonFieldType.STRING).description("작성자 프로필 이미지 URL").optional(),
                                fieldWithPath("data.comments[].createdAt").type(JsonFieldType.STRING).description("작성 시각"),
                                fieldWithPath("data.comments[].updatedAt").type(JsonFieldType.STRING).description("수정 시각"),
                                fieldWithPath("data.offset").type(JsonFieldType.NUMBER).description("요청 offset"),
                                fieldWithPath("data.limit").type(JsonFieldType.NUMBER).description("요청 limit"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    void 비회원도_댓글_목록을_조회할_수_있다() throws Exception {
        when(commentQueryService.getComments(isNull(), any())).thenReturn(new CommentPageResponse(
                List.of(),
                0,
                20,
                false
        ));

        mockMvc.perform(get("/api/v1/comments")
                        .queryParam("postId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.comments").isArray());
    }

    @Test
    void 대댓글_목록_조회() throws Exception {
        when(commentQueryService.getComments(anyLong(), any())).thenReturn(new CommentPageResponse(
                List.of(createCommentResponse(201L, 101L, 1, false, false)),
                20,
                20,
                false
        ));

        mockMvc.perform(get("/api/v1/comments")
                        .session(createSession())
                        .queryParam("postId", "10")
                        .queryParam("parentCommentId", "101")
                        .queryParam("offset", "20")
                        .queryParam("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.comments[0].parentCommentId").value(101))
                .andDo(document("comments/get-replies",
                        queryParameters(
                                parameterWithName("postId").description("게시글 ID"),
                                parameterWithName("parentCommentId").description("대댓글을 조회할 부모 댓글 ID"),
                                parameterWithName("offset").description("조회 시작 위치").optional(),
                                parameterWithName("limit").description("조회할 대댓글 수").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.comments").type(JsonFieldType.ARRAY).description("대댓글 목록"),
                                fieldWithPath("data.comments[].commentId").type(JsonFieldType.NUMBER).description("댓글 ID"),
                                fieldWithPath("data.comments[].postId").type(JsonFieldType.NUMBER).description("게시글 ID"),
                                fieldWithPath("data.comments[].parentCommentId").type(JsonFieldType.NUMBER).description("부모 댓글 ID"),
                                fieldWithPath("data.comments[].depth").type(JsonFieldType.NUMBER).description("댓글 depth"),
                                fieldWithPath("data.comments[].content").type(JsonFieldType.STRING).description("댓글 내용"),
                                fieldWithPath("data.comments[].status").type(JsonFieldType.STRING).description("댓글 상태"),
                                fieldWithPath("data.comments[].isAnonymous").type(JsonFieldType.BOOLEAN).description("익명 여부"),
                                fieldWithPath("data.comments[].isMine").type(JsonFieldType.BOOLEAN).description("내 댓글 여부"),
                                fieldWithPath("data.comments[].likedByMe").type(JsonFieldType.BOOLEAN).description("내가 좋아요한 댓글인지 여부"),
                                fieldWithPath("data.comments[].blockedAuthor").type(JsonFieldType.BOOLEAN).description("차단한 작성자의 댓글인지 여부"),
                                fieldWithPath("data.comments[].likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                                fieldWithPath("data.comments[].replyCount").type(JsonFieldType.NUMBER).description("직계 대댓글 수"),
                                fieldWithPath("data.comments[].author.memberId").type(JsonFieldType.NUMBER).description("작성자 회원 ID").optional(),
                                fieldWithPath("data.comments[].author.nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                                fieldWithPath("data.comments[].author.profileImageUrl").type(JsonFieldType.STRING).description("작성자 프로필 이미지 URL").optional(),
                                fieldWithPath("data.comments[].createdAt").type(JsonFieldType.STRING).description("작성 시각"),
                                fieldWithPath("data.comments[].updatedAt").type(JsonFieldType.STRING).description("수정 시각"),
                                fieldWithPath("data.offset").type(JsonFieldType.NUMBER).description("요청 offset"),
                                fieldWithPath("data.limit").type(JsonFieldType.NUMBER).description("요청 limit"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    void 댓글_작성() throws Exception {
        when(commentService.createComment(anyLong(), anyLong(), any(), any())).thenReturn(301L);
        when(commentQueryService.getComment(1L, 301L)).thenReturn(
                createCommentResponse(301L, 101L, 1, true, false)
        );

        mockMvc.perform(post("/api/v1/comments")
                        .session(createSession())
                        .queryParam("postId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "parentCommentId": 101,
                                    "content": "저도 참여하고 싶어요.",
                                    "isAnonymous": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.commentId").value(301))
                .andDo(document("comments/create-comment",
                        queryParameters(
                                parameterWithName("postId").description("댓글을 작성할 게시글 ID")
                        ),
                        requestFields(
                                fieldWithPath("parentCommentId").type(JsonFieldType.NUMBER).description("부모 댓글 ID. 일반 댓글이면 null").optional(),
                                fieldWithPath("content").type(JsonFieldType.STRING).description("댓글 내용"),
                                fieldWithPath("isAnonymous").type(JsonFieldType.BOOLEAN).description("익명 여부")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.commentId").type(JsonFieldType.NUMBER).description("생성된 댓글 ID"),
                                fieldWithPath("data.postId").type(JsonFieldType.NUMBER).description("게시글 ID"),
                                fieldWithPath("data.parentCommentId").type(JsonFieldType.NUMBER).description("부모 댓글 ID").optional(),
                                fieldWithPath("data.depth").type(JsonFieldType.NUMBER).description("댓글 depth"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING).description("댓글 내용"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("댓글 상태"),
                                fieldWithPath("data.isAnonymous").type(JsonFieldType.BOOLEAN).description("익명 여부"),
                                fieldWithPath("data.isMine").type(JsonFieldType.BOOLEAN).description("내 댓글 여부"),
                                fieldWithPath("data.likedByMe").type(JsonFieldType.BOOLEAN).description("내가 좋아요한 댓글인지 여부"),
                                fieldWithPath("data.blockedAuthor").type(JsonFieldType.BOOLEAN).description("차단한 작성자의 댓글인지 여부"),
                                fieldWithPath("data.likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                                fieldWithPath("data.replyCount").type(JsonFieldType.NUMBER).description("직계 대댓글 수"),
                                fieldWithPath("data.author.memberId").type(JsonFieldType.NULL).description("익명 댓글인 경우 null"),
                                fieldWithPath("data.author.nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                                fieldWithPath("data.author.profileImageUrl").type(JsonFieldType.NULL).description("익명 댓글인 경우 null"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("작성 시각"),
                                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정 시각")
                        )
                ));
    }

    @Test
    void 댓글_수정() throws Exception {
        when(commentService.updateComment(anyLong(), anyLong(), any())).thenReturn(101L);
        when(commentQueryService.getComment(1L, 101L)).thenReturn(
                createCommentResponse(101L, null, 0, false, false)
        );

        mockMvc.perform(put("/api/v1/comments/{commentId}", 101L)
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "content": "수정한 댓글입니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentId").value(101))
                .andDo(document("comments/update-comment",
                        pathParameters(
                                parameterWithName("commentId").description("수정할 댓글 ID")
                        ),
                        requestFields(
                                fieldWithPath("content").type(JsonFieldType.STRING).description("수정할 댓글 내용")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.commentId").type(JsonFieldType.NUMBER).description("댓글 ID"),
                                fieldWithPath("data.postId").type(JsonFieldType.NUMBER).description("게시글 ID"),
                                fieldWithPath("data.parentCommentId").type(JsonFieldType.NULL).description("부모 댓글 ID").optional(),
                                fieldWithPath("data.depth").type(JsonFieldType.NUMBER).description("댓글 depth"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING).description("수정된 댓글 내용"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("댓글 상태"),
                                fieldWithPath("data.isAnonymous").type(JsonFieldType.BOOLEAN).description("익명 여부"),
                                fieldWithPath("data.isMine").type(JsonFieldType.BOOLEAN).description("내 댓글 여부"),
                                fieldWithPath("data.likedByMe").type(JsonFieldType.BOOLEAN).description("내가 좋아요한 댓글인지 여부"),
                                fieldWithPath("data.blockedAuthor").type(JsonFieldType.BOOLEAN).description("차단한 작성자의 댓글인지 여부"),
                                fieldWithPath("data.likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                                fieldWithPath("data.replyCount").type(JsonFieldType.NUMBER).description("직계 대댓글 수"),
                                fieldWithPath("data.author.memberId").type(JsonFieldType.NUMBER).description("작성자 회원 ID").optional(),
                                fieldWithPath("data.author.nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                                fieldWithPath("data.author.profileImageUrl").type(JsonFieldType.STRING).description("작성자 프로필 이미지 URL").optional(),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("작성 시각"),
                                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정 시각")
                        )
                ));
    }

    @Test
    void 댓글_삭제() throws Exception {
        when(commentService.deleteComment(1L, 101L)).thenReturn(
                CommentDeleteResponse.delete(10L, 101L, CommentStatus.DELETED)
        );

        mockMvc.perform(delete("/api/v1/comments/{commentId}", 101L)
                        .session(createSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"))
                .andDo(document("comments/delete-comment",
                        pathParameters(
                                parameterWithName("commentId").description("삭제할 댓글 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.postId").type(JsonFieldType.NUMBER).description("댓글이 속한 게시글 ID"),
                                fieldWithPath("data.commentId").type(JsonFieldType.NUMBER).description("댓글 ID"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("삭제 후 댓글 상태")
                        )
                ));
    }

    private CommentResponse createCommentResponse(
            Long commentId,
            Long parentCommentId,
            int depth,
            boolean anonymous,
            boolean likedByMe
    ) {
        return new CommentResponse(
                commentId,
                10L,
                parentCommentId,
                depth,
                "저도 참여하고 싶어요.",
                CommentStatus.ACTIVE,
                anonymous,
                false,
                likedByMe,
                false,
                3L,
                2L,
                CommentAuthorResponse.visibleOf(
                        anonymous,
                        false,
                        2L,
                        "luna",
                        "https://cdn.example.com/profile.png"
                ),
                LocalDateTime.of(2026, 3, 15, 12, 0),
                LocalDateTime.of(2026, 3, 15, 12, 5)
        );
    }

    private MockHttpSession createSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "tester", MemberRole.MEMBER));
        return session;
    }
}
