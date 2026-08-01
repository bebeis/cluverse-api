package cluverse.comment.repository;

import cluverse.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {

    boolean existsByParentId(Long parentId);

    Optional<Comment> findByMemberIdAndClientRequestId(Long memberId, String clientRequestId);
}
