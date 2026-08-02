package cluverse.comment.service;

import cluverse.comment.domain.CommentPageCursor;
import cluverse.comment.domain.CommentStatus;
import cluverse.comment.repository.dto.CommentPageQueryResult;
import cluverse.comment.repository.dto.CommentQueryDto;
import cluverse.comment.service.implement.CommentProcessor;
import cluverse.comment.service.implement.CommentReader;
import cluverse.comment.service.implement.CommentWriter;
import cluverse.comment.service.request.CommentCreateRequest;
import cluverse.comment.service.request.CommentPageRequest;
import cluverse.comment.service.request.CommentUpdateRequest;
import cluverse.comment.service.response.CommentDeleteResponse;
import cluverse.comment.service.response.CommentLastRepliedPost;
import cluverse.comment.service.response.CommentPageResponse;
import cluverse.comment.service.response.CommentResponse;
import cluverse.member.service.implement.MemberReader;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.popularity.domain.PopularityTrigger;
import cluverse.popularity.service.implement.PopularityPromotionInvoker;
import cluverse.post.service.implement.PostAccessReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceV1Test {

    @Mock
    private CommentReader commentReader;

    @Mock
    private CommentWriter commentWriter;

    @Mock
    private MemberReader memberReader;

    @Mock
    private PostAccessReader postAccessReader;

    @Mock
    private PostMetaWriter postMetaWriter;

    @Mock
    private CommentProcessor commentProcessor;

    @Mock
    private PopularityPromotionInvoker popularityPromotionInvoker;

    @InjectMocks
    private CommentQueryService commentQueryService;

    @InjectMocks
    private CommentService commentService;

    @Test
    void 댓글_목록_조회시_쿼리_결과를_응답으로_조립한다() {
        // given
        CommentPageRequest request = new CommentPageRequest(10L, null, null, 20);
        when(commentReader.readMaxCommentId()).thenReturn(200L);
        when(commentReader.readCommentPageV1(eq(99L), eq(request), any(CommentPageCursor.class)))
                .thenReturn(new CommentPageQueryResult(
                List.of(createCommentQueryDto(101L, null, 0, true, false)),
                true,
                "20260315100000-00000000000000000101"
                ));

        // when
        CommentPageResponse response = commentQueryService.getCommentsV1(99L, request);

        // then
        assertThat(response.comments()).extracting(CommentResponse::commentId).containsExactly(101L);
        assertThat(response.nextCursor()).isNotBlank();
        assertThat(response.limit()).isEqualTo(20);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.comments().getFirst().author().nickname()).isEqualTo("익명");
        verify(postAccessReader).validateReadablePost(99L, 10L);
        ArgumentCaptor<CommentPageCursor> cursorCaptor = ArgumentCaptor.forClass(CommentPageCursor.class);
        verify(commentReader).readCommentPageV1(eq(99L), eq(request), cursorCaptor.capture());
        assertThat(cursorCaptor.getValue().path()).isEmpty();
        assertThat(cursorCaptor.getValue().snapshotMaxCommentId()).isEqualTo(200L);
        assertThat(cursorCaptor.getValue().asOf()).isNotNull();
    }

    @Test
    void 댓글_작성은_Processor에_위임하고_생성된_ID를_반환한다() {
        // given
        CommentCreateRequest request = new CommentCreateRequest(100L, "대댓글입니다.", false);
        when(commentProcessor.createComment(1L, 10L, request, "127.0.0.1")).thenReturn(101L);

        // when
        Long commentId = commentService.createComment(1L, 10L, request, "127.0.0.1");

        // then
        assertThat(commentId).isEqualTo(101L);
        verify(commentProcessor).createComment(1L, 10L, request, "127.0.0.1");
        verify(popularityPromotionInvoker).tryEvaluate(10L, PopularityTrigger.COMMENT);
    }

    @Test
    void 댓글_수정은_Writer에_위임하고_댓글ID를_반환한다() {
        // given
        CommentUpdateRequest request = new CommentUpdateRequest("수정된 댓글입니다.");

        // when
        Long commentId = commentService.updateComment(1L, 101L, request);

        // then
        assertThat(commentId).isEqualTo(101L);
        verify(commentWriter).update(1L, 101L, request);
    }

    @Test
    void 댓글_삭제는_Processor에_위임하고_삭제_응답을_조립한다() {
        // given
        when(commentProcessor.deleteComment(1L, 101L)).thenReturn(10L);

        // when
        CommentDeleteResponse response = commentService.deleteComment(1L, 101L);

        // then
        assertThat(response.postId()).isEqualTo(10L);
        assertThat(response.commentId()).isEqualTo(101L);
        assertThat(response.status()).isEqualTo(CommentStatus.DELETED);
        verify(commentProcessor).deleteComment(1L, 101L);
    }

    @Test
    void 최근_댓글_게시글_조회시_쿼리_결과를_그대로_반환한다() {
        // given
        List<CommentLastRepliedPost> expected = List.of(
                new CommentLastRepliedPost(20L, LocalDateTime.of(2026, 3, 20, 12, 0)),
                new CommentLastRepliedPost(10L, LocalDateTime.of(2026, 3, 20, 11, 0))
        );
        when(commentReader.readRecentCommentRepliedPosts(2L)).thenReturn(expected);

        // when
        List<CommentLastRepliedPost> result = commentQueryService.getRecentCommentRepliedPostIds(2L);

        // then
        assertThat(result).isEqualTo(expected);
        verify(commentReader).readRecentCommentRepliedPosts(2L);
    }

    private CommentQueryDto createCommentQueryDto(
            Long commentId,
            Long parentCommentId,
            int depth,
            boolean isAnonymous,
            boolean blockedAuthor
    ) {
        return new CommentQueryDto(
                commentId,
                10L,
                parentCommentId,
                depth,
                "댓글 내용",
                CommentStatus.ACTIVE,
                isAnonymous,
                3L,
                2L,
                2L,
                "writer",
                "https://cdn.example.com/profile.png",
                false,
                blockedAuthor,
                LocalDateTime.of(2026, 3, 15, 10, 0),
                LocalDateTime.of(2026, 3, 15, 10, 5)
        );
    }
}
