package cluverse.popularity.repository;

import cluverse.popularity.domain.PopularityFinalizationClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface PopularityFinalizationClaimRepository extends JpaRepository<PopularityFinalizationClaim, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO popularity_finalization_claim (
                post_id, claim_token, claimed_at, created_at, updated_at
            ) VALUES (
                :postId, :claimToken, :claimedAt, :claimedAt, :claimedAt
            )
            ON DUPLICATE KEY UPDATE
                claim_token = CASE WHEN claimed_at < :staleBefore THEN :claimToken ELSE claim_token END,
                claimed_at = CASE WHEN claimed_at < :staleBefore THEN :claimedAt ELSE claimed_at END,
                updated_at = CASE WHEN claimed_at < :staleBefore THEN :claimedAt ELSE updated_at END
            """, nativeQuery = true)
    int tryClaim(
            @Param("postId") Long postId,
            @Param("claimToken") String claimToken,
            @Param("claimedAt") LocalDateTime claimedAt,
            @Param("staleBefore") LocalDateTime staleBefore
    );

    boolean existsByPostIdAndClaimToken(Long postId, String claimToken);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from PopularityFinalizationClaim claim
            where claim.postId = :postId
              and claim.claimToken = :claimToken
            """)
    int release(@Param("postId") Long postId, @Param("claimToken") String claimToken);
}
