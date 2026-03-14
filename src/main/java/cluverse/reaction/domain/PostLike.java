package cluverse.reaction.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_like_id")
    private Long id;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long memberId;

    private PostLike(Long postId, Long memberId) {
        this.postId = postId;
        this.memberId = memberId;
    }

    public static PostLike of(Long postId, Long memberId) {
        return new PostLike(postId, memberId);
    }
}
