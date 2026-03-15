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
@Table(name = "post_bookmark_count")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostBookmarkCount extends BaseTimeEntity {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(nullable = false)
    private int bookmarkCount;

    private PostBookmarkCount(Long postId, int bookmarkCount) {
        this.postId = postId;
        this.bookmarkCount = bookmarkCount;
    }

    public static PostBookmarkCount of(Long postId, int bookmarkCount) {
        return new PostBookmarkCount(postId, bookmarkCount);
    }
}
