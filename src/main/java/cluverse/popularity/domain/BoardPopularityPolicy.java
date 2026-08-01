package cluverse.popularity.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_popularity_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardPopularityPolicy extends BaseTimeEntity {

    @Id
    @Column(name = "board_id")
    private Long boardId;

    @Column(nullable = false)
    private long promotionScore;

    @Column(nullable = false)
    private int likeGate;

    @Column(nullable = false)
    private int commentGate;

    @Column(nullable = false)
    private int sampleSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PopularityPolicySource policySource;

    @Column(nullable = false)
    private LocalDateTime computedAt;

    public static BoardPopularityPolicy create(
            Long boardId,
            long promotionScore,
            int likeGate,
            int commentGate,
            int sampleSize,
            PopularityPolicySource policySource,
            LocalDateTime computedAt
    ) {
        BoardPopularityPolicy policy = new BoardPopularityPolicy();
        policy.boardId = boardId;
        policy.replace(promotionScore, likeGate, commentGate, sampleSize, policySource, computedAt);
        return policy;
    }

    public void replace(
            long promotionScore,
            int likeGate,
            int commentGate,
            int sampleSize,
            PopularityPolicySource policySource,
            LocalDateTime computedAt
    ) {
        this.promotionScore = promotionScore;
        this.likeGate = likeGate;
        this.commentGate = commentGate;
        this.sampleSize = sampleSize;
        this.policySource = policySource;
        this.computedAt = computedAt;
    }
}
