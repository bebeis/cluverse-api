package cluverse.post.repository;

import cluverse.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Post post
            set post.viewCount = post.viewCount + 1
            where post.id = :postId
            and post.status = cluverse.post.domain.PostStatus.ACTIVE
            """)
    int increaseViewCount(@Param("postId") Long postId);

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
