package cluverse.member.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "following_id"}))
public class Follow extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "following_id", nullable = false)
    private Long followingId;

    private Follow(Long followerId, Long followingId) {
        this.followerId = followerId;
        this.followingId = followingId;
    }

    public static Follow of(Long followerId, Long followingId) {
        return new Follow(followerId, followingId);
    }
}
