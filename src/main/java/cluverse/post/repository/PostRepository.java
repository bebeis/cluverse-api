package cluverse.post.repository;

import cluverse.post.domain.Post;
import cluverse.post.domain.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    boolean existsByIdAndStatus(Long id, PostStatus status);
}
