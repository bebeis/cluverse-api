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
@Table(name = "post_view_count_v2")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostViewCountV2 extends BaseTimeEntity {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(nullable = false)
    private int viewCount;

    @Version
    @Column(nullable = false)
    private Long version;

    private PostViewCountV2(Long postId, int viewCount) {
        this.postId = postId;
        this.viewCount = viewCount;
    }

    public static PostViewCountV2 create(Long postId) {
        return new PostViewCountV2(postId, 0);
    }

    public void increase() {
        viewCount += 1;
    }
}
