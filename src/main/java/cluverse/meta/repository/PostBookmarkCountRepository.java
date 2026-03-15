package cluverse.meta.repository;

import cluverse.meta.domain.PostBookmarkCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PostBookmarkCountRepository extends JpaRepository<PostBookmarkCount, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO post_bookmark_count (post_id, bookmark_count, created_at, updated_at)
            VALUES (:postId, 1, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                bookmark_count = bookmark_count + 1,
                updated_at = NOW()
            """, nativeQuery = true)
    void increaseCount(@Param("postId") Long postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select postBookmarkCount
            from PostBookmarkCount postBookmarkCount
            where postBookmarkCount.postId = :postId
            """)
    Optional<PostBookmarkCount> findByPostIdForUpdate(@Param("postId") Long postId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE post_bookmark_count
            SET bookmark_count = bookmark_count - 1,
                updated_at = NOW()
            WHERE post_id = :postId
              AND bookmark_count > 0
            """, nativeQuery = true)
    int decreaseCount(@Param("postId") Long postId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM post_bookmark_count
            WHERE post_id = :postId
              AND bookmark_count = 0
            """, nativeQuery = true)
    void deleteIfZero(@Param("postId") Long postId);
}
