package cluverse.popularity.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "popular_post",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_popular_post_version_post",
                columnNames = {"algorithm_version", "post_id"}
        ),
        indexes = {
                @Index(name = "idx_popular_recent", columnList = "algorithm_version,finalized_at,promoted_at,post_id"),
                @Index(name = "idx_popular_ranking", columnList = "algorithm_version,finalized_at,score,post_id"),
                @Index(name = "idx_popular_finalize_due", columnList = "finalized_at,finalize_at,post_id"),
                @Index(name = "idx_popular_finalize_post", columnList = "post_id,finalized_at,finalize_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopularPost extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "popular_post_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PopularityAlgorithmVersion algorithmVersion;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long boardId;

    @Column(nullable = false)
    private LocalDateTime promotedAt;

    @Column(nullable = false)
    private LocalDateTime finalizeAt;

    @Column(nullable = false)
    private long scoreAtPromotion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PopularityTrigger promotionTrigger;

    @Column(nullable = false)
    private long promotionScoreThreshold;

    @Column(nullable = false)
    private int likeGateThreshold;

    @Column(nullable = false)
    private int commentGateThreshold;

    private Long score;
    private Long likeCount;
    private Long commentCount;
    private Long viewCount;
    private LocalDateTime finalizedAt;
}
