package cluverse.meta.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_view_count_optimistic")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostViewCountOptimistic extends BaseTimeEntity {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(nullable = false)
    private int viewCount;

    @Version
    @Column(nullable = false)
    private Long version;

    private PostViewCountOptimistic(Long postId, int viewCount) {
        this.postId = postId;
        this.viewCount = viewCount;
    }

    public static PostViewCountOptimistic create(Long postId) {
        return new PostViewCountOptimistic(postId, 0);
    }

    public void increase() {
        viewCount += 1;
    }
}
