package cluverse.meta.repository;

import cluverse.meta.domain.PostCommentCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PostCommentCountRepository extends JpaRepository<PostCommentCount, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO post_comment_count (post_id, comment_count, created_at, updated_at)
            VALUES (:postId, 1, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                comment_count = comment_count + 1,
                updated_at = NOW()
            """, nativeQuery = true)
    void increaseCount(@Param("postId") Long postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select postCommentCount
            from PostCommentCount postCommentCount
            where postCommentCount.postId = :postId
            """)
    Optional<PostCommentCount> findByPostIdForUpdate(@Param("postId") Long postId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE post_comment_count
            SET comment_count = comment_count - 1,
                updated_at = NOW()
            WHERE post_id = :postId
              AND comment_count > 0
            """, nativeQuery = true)
    int decreaseCount(@Param("postId") Long postId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM post_comment_count
            WHERE post_id = :postId
              AND comment_count = 0
            """, nativeQuery = true)
    void deleteIfZero(@Param("postId") Long postId);
}
