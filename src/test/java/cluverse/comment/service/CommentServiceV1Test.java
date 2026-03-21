package cluverse.comment.service;

import cluverse.comment.domain.Comment;
import cluverse.comment.domain.CommentStatus;
import cluverse.comment.repository.CommentQueryRepository;
import cluverse.comment.repository.dto.CommentPageQueryResult;
import cluverse.comment.repository.dto.CommentQueryDto;
import cluverse.comment.service.implement.CommentReader;
import cluverse.comment.service.implement.CommentWriter;
import cluverse.comment.service.request.CommentCreateRequest;
import cluverse.comment.service.request.CommentPageRequest;
import cluverse.comment.service.request.CommentUpdateRequest;
import cluverse.comment.service.response.CommentLastRepliedPost;
import cluverse.comment.service.response.CommentPageResponse;
import cluverse.comment.service.response.CommentResponse;
import cluverse.common.exception.ForbiddenException;
import cluverse.member.service.MemberService;
import cluverse.meta.service.PostMetaService;
import cluverse.post.service.PostAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceV1Test {

    @Mock
    private CommentReader commentReader;

    @Mock
    private CommentWriter commentWriter;

    @Mock
    private CommentQueryRepository commentQueryRepository;

    @Mock
    private PostAccessService postAccessService;

    @Mock
    private PostMetaService postMetaService;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private CommentServiceV1 commentService;

    @Test
    void 댓글_목록_조회시_쿼리_결과를_응답으로_조립한다() {
        // given
        CommentPageRequest request = new CommentPageRequest(10L, null, 0, 20);
        when(commentQueryRepository.findCommentPage(99L, request)).thenReturn(new CommentPageQueryResult(
                List.of(createCommentQueryDto(101L, null, 0, true, false)),
                true
        ));

        // when
        CommentPageResponse response = commentService.getComments(99L, request);

        // then
        assertThat(response.comments()).extracting(CommentResponse::commentId).containsExactly(101L);
        assertThat(response.offset()).isEqualTo(0);
        assertThat(response.limit()).isEqualTo(20);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.comments().getFirst().author().nickname()).isEqualTo("익명");
        verify(postAccessService).validateReadablePost(99L, 10L);
    }

    @Test
    void 대댓글_작성시_게시글_댓글수와_부모_대댓글수를_증가시킨다() {
        // given
        CommentCreateRequest request = new CommentCreateRequest(100L, "대댓글입니다.", false);
        Comment parentComment = createComment(100L, 10L, 2L, null, 0);
        Comment createdComment = createComment(101L, 10L, 1L, 100L, 1);

        when(commentReader.readOrThrow(100L)).thenReturn(parentComment);
        when(commentWriter.create(1L, 10L, parentComment, request, "127.0.0.1")).thenReturn(createdComment);
        when(commentQueryRepository.findComment(1L, 101L)).thenReturn(createCommentQueryDto(101L, 100L, 1, false, false));

        // when
        CommentResponse response = commentService.createComment(1L, 10L, request, "127.0.0.1");

        // then
        assertThat(response.commentId()).isEqualTo(101L);
        verify(postAccessService).validateWritablePost(1L, 10L);
        verify(commentWriter).increaseReplyCount(100L);
        verify(postMetaService).increaseCommentCount(10L);
    }

    @Test
    void 댓글_수정시_수정된_댓글을_응답으로_반환한다() {
        // given
        Comment comment = createComment(101L, 10L, 1L, null, 0);
        CommentUpdateRequest request = new CommentUpdateRequest("수정된 댓글입니다.");

        when(commentReader.readActiveOrThrow(101L)).thenReturn(comment);
        when(commentQueryRepository.findComment(1L, 101L)).thenReturn(createCommentQueryDto(101L, null, 0, false, false));

        // when
        CommentResponse response = commentService.updateComment(1L, 101L, request);

        // then
        assertThat(response.commentId()).isEqualTo(101L);
        verify(commentWriter).update(comment, request);
    }

    @Test
    void 작성자가_아니면_댓글을_수정할_수_없다() {
        // given
        Comment comment = createComment(101L, 10L, 2L, null, 0);
        when(commentReader.readActiveOrThrow(101L)).thenReturn(comment);

        // when & then
        assertThatThrownBy(() -> commentService.updateComment(1L, 101L, new CommentUpdateRequest("수정")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void 작성자도_관리자도_아니면_댓글을_삭제할_수_없다() {
        // given
        Comment comment = createComment(101L, 10L, 2L, null, 0);
        when(commentReader.readOrThrow(101L)).thenReturn(comment);
        when(memberService.isAdmin(1L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(1L, 101L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void 자식이_있는_댓글을_삭제하면_soft_delete만_수행한다() {
        // given
        Comment comment = createComment(101L, 10L, 1L, null, 0);
        when(commentReader.readOrThrow(101L)).thenReturn(comment);
        when(commentReader.hasChildren(comment)).thenReturn(true);

        // when
        commentService.deleteComment(1L, 101L);

        // then
        verify(commentWriter).delete(comment);
        verify(commentWriter, never()).remove(any());
        verify(postMetaService, never()).decreaseCommentCount(anyLong());
    }

    @Test
    void 자식이_없는_삭제된_부모는_자식_삭제시_함께_실제_삭제된다() {
        // given
        Comment child = createComment(102L, 10L, 1L, 101L, 1);
        Comment deletedParent = createDeletedComment(101L, 10L, 1L, null, 0);

        when(commentReader.readOrThrow(102L)).thenReturn(child);
        when(commentReader.hasChildren(child)).thenReturn(false);
        when(commentReader.read(101L)).thenReturn(java.util.Optional.of(deletedParent));
        when(commentReader.hasChildren(deletedParent)).thenReturn(false);

        // when
        commentService.deleteComment(1L, 102L);

        // then
        verify(commentWriter).remove(child);
        verify(commentWriter).decreaseReplyCount(101L);
        verify(commentWriter).remove(deletedParent);
        verify(postMetaService, times(2)).decreaseCommentCount(10L);
    }

    @Test
    void 최근_댓글_게시글_조회시_쿼리_결과를_그대로_반환한다() {
        // given
        List<CommentLastRepliedPost> expected = List.of(
                new CommentLastRepliedPost(20L, LocalDateTime.of(2026, 3, 20, 12, 0)),
                new CommentLastRepliedPost(10L, LocalDateTime.of(2026, 3, 20, 11, 0))
        );
        when(commentQueryRepository.findRecentCommentRepliedPosts(2L)).thenReturn(expected);

        // when
        List<CommentLastRepliedPost> result = commentService.getRecentCommentRepliedPostIds(2L);

        // then
        assertThat(result).isEqualTo(expected);
        verify(commentQueryRepository).findRecentCommentRepliedPosts(2L);
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

    private Comment createComment(Long commentId, Long postId, Long memberId, Long parentId, int depth) {
        Comment comment = Comment.createByMember(postId, memberId, parentId, depth, "댓글 내용", false, "127.0.0.1");
        ReflectionTestUtils.setField(comment, "id", commentId);
        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.of(2026, 3, 15, 10, 0));
        ReflectionTestUtils.setField(comment, "updatedAt", LocalDateTime.of(2026, 3, 15, 10, 0));
        return comment;
    }

    private Comment createDeletedComment(Long commentId, Long postId, Long memberId, Long parentId, int depth) {
        Comment comment = createComment(commentId, postId, memberId, parentId, depth);
        comment.delete();
        return comment;
    }
}
