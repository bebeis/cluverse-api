package cluverse.meta.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_comment_count")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostCommentCount extends BaseTimeEntity {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(nullable = false)
    private int commentCount;

    private PostCommentCount(Long postId, int commentCount) {
        this.postId = postId;
        this.commentCount = commentCount;
    }

    public static PostCommentCount of(Long postId, int commentCount) {
        return new PostCommentCount(postId, commentCount);
    }
}
