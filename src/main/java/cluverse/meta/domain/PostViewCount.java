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
@Table(name = "post_view_count")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostViewCount extends BaseTimeEntity {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(nullable = false)
    private int viewCount;

    private PostViewCount(Long postId, int viewCount) {
        this.postId = postId;
        this.viewCount = viewCount;
    }

    public static PostViewCount of(Long postId, int viewCount) {
        return new PostViewCount(postId, viewCount);
    }
}
