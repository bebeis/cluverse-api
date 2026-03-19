package cluverse.meta.repository;

import cluverse.meta.domain.PostViewCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostViewCountRepository extends JpaRepository<PostViewCount, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE post_view_count
            SET view_count = view_count + 1,
                updated_at = NOW()
            WHERE post_id = :postId
            """, nativeQuery = true)
    int increaseCount(@Param("postId") Long postId);
}
