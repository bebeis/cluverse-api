package cluverse.post.repository;

import cluverse.post.domain.Post;
import cluverse.post.domain.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    boolean existsByIdAndStatus(Long id, PostStatus status);

    Optional<Post> findByMemberIdAndClientRequestId(Long memberId, String clientRequestId);
}
