package cluverse.meta.repository;

import cluverse.meta.domain.PostLikeCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PostLikeCountRepository extends JpaRepository<PostLikeCount, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO post_like_count (post_id, like_count, created_at, updated_at)
            VALUES (:postId, 1, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                like_count = like_count + 1,
                updated_at = NOW()
            """, nativeQuery = true)
    void increaseCount(@Param("postId") Long postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select postLikeCount
            from PostLikeCount postLikeCount
            where postLikeCount.postId = :postId
            """)
    Optional<PostLikeCount> findByPostIdForUpdate(@Param("postId") Long postId);
}
