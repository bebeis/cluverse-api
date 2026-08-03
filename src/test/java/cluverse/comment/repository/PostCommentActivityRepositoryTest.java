package cluverse.comment.repository;

import cluverse.comment.domain.Comment;
import cluverse.comment.domain.PostCommentActivity;
import cluverse.comment.service.implement.PostCommentActivityWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PostCommentActivityWriter.class)
class PostCommentActivityRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostCommentActivityRepository activityRepository;

    @Autowired
    private PostCommentActivityWriter activityWriter;

    @Test
    void 늦게_도착한_과거_댓글은_최신_활동을_뒤로_돌리지_않는다() {
        // given
        Comment older = saveComment(10L, "이전 댓글");
        Comment latest = saveComment(10L, "최신 댓글");

        // when
        activityWriter.reflectCreated(latest);
        activityWriter.reflectCreated(older);

        // then
        PostCommentActivity activity = activityRepository.findById(10L).orElseThrow();
        assertThat(activity.getLastCommentId()).isEqualTo(latest.getId());
        assertThat(activity.getLastCommentedAt())
                .isEqualTo(latest.getCreatedAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void 생성_시각이_과거인_높은_ID_댓글은_최신_활동을_뒤로_돌리지_않는다() {
        // given
        LocalDateTime latestTime = LocalDateTime.of(2026, 8, 3, 12, 0);
        LocalDateTime olderTime = latestTime.minusSeconds(1);
        Comment latestByTime = saveComment(15L, "시각 기준 최신 댓글", latestTime);
        Comment higherIdButOlder = saveComment(15L, "ID만 큰 이전 댓글", olderTime);

        // when
        activityWriter.reflectCreated(latestByTime);
        activityWriter.reflectCreated(higherIdButOlder);

        // then
        PostCommentActivity activity = activityRepository.findById(15L).orElseThrow();
        assertThat(activity.getLastCommentId()).isEqualTo(latestByTime.getId());
        assertThat(activity.getLastCommentedAt()).isEqualTo(latestTime);
    }

    @Test
    void 최신_댓글을_삭제하면_남아있는_이전_댓글로_활동을_되돌린다() {
        // given
        Comment older = saveComment(20L, "이전 댓글");
        Comment latest = saveComment(20L, "최신 댓글");
        activityWriter.reflectCreated(older);
        activityWriter.reflectCreated(latest);

        // when
        commentRepository.delete(latest);
        activityWriter.reflectDeleted(20L, latest.getId());

        // then
        PostCommentActivity activity = activityRepository.findById(20L).orElseThrow();
        assertThat(activity.getLastCommentId()).isEqualTo(older.getId());
        assertThat(activity.getLastCommentedAt())
                .isEqualTo(older.getCreatedAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void 마지막_댓글을_삭제하면_게시글_활동도_삭제한다() {
        // given
        Comment onlyComment = saveComment(30L, "유일한 댓글");
        activityWriter.reflectCreated(onlyComment);

        // when
        commentRepository.delete(onlyComment);
        activityWriter.reflectDeleted(30L, onlyComment.getId());

        // then
        assertThat(activityRepository.findById(30L)).isEmpty();
    }

    private Comment saveComment(Long postId, String content) {
        return commentRepository.saveAndFlush(Comment.createByMember(
                postId, 1L, null, 0, content, false, "127.0.0.1"
        ));
    }

    private Comment saveComment(Long postId, String content, LocalDateTime createdAt) {
        Comment comment = Comment.createByMember(
                postId, 1L, null, 0, content, false, "127.0.0.1"
        );
        Comment savedComment = commentRepository.saveAndFlush(comment);
        ReflectionTestUtils.setField(savedComment, "createdAt", createdAt);
        ReflectionTestUtils.setField(savedComment, "updatedAt", createdAt);
        return savedComment;
    }
}
