package cluverse.post.repository;

import cluverse.post.domain.Post;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select post
            from Post post
            where post.id = :postId
            """)
    Optional<Post> findByIdForUpdate(@Param("postId") Long postId);

    @Query("""
            select distinct post
            from Post post
            left join fetch post.images
            where post.id = :postId
            """)
    Optional<Post> findWithImagesById(@Param("postId") Long postId);

    @Query("""
            select distinct post
            from Post post
            left join fetch post.images
            where post.id in :postIds
            """)
    List<Post> findAllWithImagesByIdIn(@Param("postIds") Collection<Long> postIds);
}
