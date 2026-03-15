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
@Table(name = "post_like_count")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLikeCount extends BaseTimeEntity {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(nullable = false)
    private int likeCount;

    private PostLikeCount(Long postId, int likeCount) {
        this.postId = postId;
        this.likeCount = likeCount;
    }

    public static PostLikeCount of(Long postId, int likeCount) {
        return new PostLikeCount(postId, likeCount);
    }
}
