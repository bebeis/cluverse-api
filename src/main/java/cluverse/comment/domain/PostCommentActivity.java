package cluverse.comment.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostCommentActivity extends BaseTimeEntity {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "last_comment_id", nullable = false)
    private Long lastCommentId;

    @Column(name = "last_commented_at", nullable = false)
    private LocalDateTime lastCommentedAt;

    private PostCommentActivity(Long postId, Long lastCommentId, LocalDateTime lastCommentedAt) {
        this.postId = postId;
        this.lastCommentId = lastCommentId;
        this.lastCommentedAt = lastCommentedAt;
    }

    public static PostCommentActivity from(Comment comment) {
        return new PostCommentActivity(
                comment.getPostId(),
                comment.getId(),
                comment.getCreatedAt()
        );
    }

    public void replaceLatest(Comment comment) {
        if (!postId.equals(comment.getPostId())) {
            throw new IllegalArgumentException("같은 게시글의 댓글로만 최근 활동을 교체할 수 있습니다.");
        }
        this.lastCommentId = comment.getId();
        this.lastCommentedAt = comment.getCreatedAt();
    }
}
