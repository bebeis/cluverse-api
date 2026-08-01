package cluverse.popularity.repository;

import cluverse.popularity.domain.PopularityCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PopularityCandidateRepository extends JpaRepository<PopularityCandidate, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO popularity_candidate (
                post_id, board_id, registered_at, next_check_at, expires_at, created_at, updated_at
            ) VALUES (
                :postId, :boardId, :registeredAt, :nextCheckAt, :expiresAt, :registeredAt, :registeredAt
            )
            ON DUPLICATE KEY UPDATE
                board_id = :boardId,
                next_check_at = LEAST(next_check_at, :nextCheckAt),
                expires_at = GREATEST(expires_at, :expiresAt),
                updated_at = :registeredAt
            """, nativeQuery = true)
    int upsert(
            @Param("postId") Long postId,
            @Param("boardId") Long boardId,
            @Param("registeredAt") LocalDateTime registeredAt,
            @Param("nextCheckAt") LocalDateTime nextCheckAt,
            @Param("expiresAt") LocalDateTime expiresAt
    );

    @Query(value = """
            SELECT *
            FROM popularity_candidate
            WHERE next_check_at <= :now
            ORDER BY next_check_at, post_id
            LIMIT :limit
            FOR UPDATE
            """, nativeQuery = true)
    List<PopularityCandidate> findDueForUpdate(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update PopularityCandidate candidate
            set candidate.lastCheckedAt = :checkedAt,
                candidate.nextCheckAt = :nextCheckAt,
                candidate.expiresAt = :expiresAt
            where candidate.postId = :postId
            """)
    int reschedule(
            @Param("postId") Long postId,
            @Param("checkedAt") LocalDateTime checkedAt,
            @Param("nextCheckAt") LocalDateTime nextCheckAt,
            @Param("expiresAt") LocalDateTime expiresAt
    );
}
