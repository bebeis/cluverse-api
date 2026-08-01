package cluverse.popularity.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "popularity_finalization_claim")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopularityFinalizationClaim extends BaseTimeEntity {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(nullable = false, length = 36)
    private String claimToken;

    @Column(nullable = false)
    private LocalDateTime claimedAt;
}
