package cluverse.popularity.repository;

import cluverse.popularity.domain.PopularPost;
import cluverse.popularity.domain.PopularityAlgorithmVersion;
import cluverse.popularity.domain.PopularityTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PopularPostRepository extends JpaRepository<PopularPost, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO popular_post (
                algorithm_version, post_id, board_id, promoted_at, finalize_at,
                score_at_promotion, promotion_trigger, promotion_score_threshold,
                created_at, updated_at
            ) VALUES (
                :#{#version.name()}, :postId, :boardId, :promotedAt, :finalizeAt,
                :scoreAtPromotion, :#{#trigger.name()}, :promotionScoreThreshold,
                :promotedAt, :promotedAt
            )
            ON DUPLICATE KEY UPDATE post_id = :postId
            """, nativeQuery = true)
    int upsertPromotion(
            @Param("version") PopularityAlgorithmVersion version,
            @Param("postId") Long postId,
            @Param("boardId") Long boardId,
            @Param("promotedAt") LocalDateTime promotedAt,
            @Param("finalizeAt") LocalDateTime finalizeAt,
            @Param("scoreAtPromotion") long scoreAtPromotion,
            @Param("trigger") PopularityTrigger trigger,
            @Param("promotionScoreThreshold") long promotionScoreThreshold
    );

    @Query(value = """
            SELECT DISTINCT post_id
            FROM popular_post
            WHERE finalized_at IS NULL
              AND finalize_at <= :now
            ORDER BY post_id
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findDuePostIdsForFinalization(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit
    );

    @Query("""
            select popularPost
            from PopularPost popularPost
            where popularPost.postId in :postIds
              and popularPost.finalizedAt is null
              and popularPost.finalizeAt <= :now
            order by popularPost.postId, popularPost.algorithmVersion
            """)
    List<PopularPost> findDueForFinalization(
            @Param("postIds") List<Long> postIds,
            @Param("now") LocalDateTime now
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update PopularPost popularPost
            set popularPost.score = :score,
                popularPost.likeCount = :likeCount,
                popularPost.commentCount = :commentCount,
                popularPost.finalizedAt = :finalizedAt
            where popularPost.id = :id
              and popularPost.finalizedAt is null
            """)
    int finalizeIfPending(
            @Param("id") Long id,
            @Param("score") long score,
            @Param("likeCount") long likeCount,
            @Param("commentCount") long commentCount,
            @Param("finalizedAt") LocalDateTime finalizedAt
    );
}
