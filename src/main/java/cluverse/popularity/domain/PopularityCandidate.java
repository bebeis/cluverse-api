package cluverse.popularity.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "popularity_candidate",
        indexes = {
                @Index(name = "idx_candidate_check", columnList = "next_check_at,post_id"),
                @Index(name = "idx_candidate_expire", columnList = "expires_at,post_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopularityCandidate extends BaseTimeEntity {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(nullable = false)
    private Long boardId;

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    @Column(nullable = false)
    private LocalDateTime nextCheckAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime lastCheckedAt;

    public static PopularityCandidate register(
            Long postId,
            Long boardId,
            LocalDateTime registeredAt,
            LocalDateTime nextCheckAt,
            LocalDateTime expiresAt
    ) {
        PopularityCandidate candidate = new PopularityCandidate();
        candidate.postId = postId;
        candidate.boardId = boardId;
        candidate.registeredAt = registeredAt;
        candidate.nextCheckAt = nextCheckAt;
        candidate.expiresAt = expiresAt;
        return candidate;
    }
}
