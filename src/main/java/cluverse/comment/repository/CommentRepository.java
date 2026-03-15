package cluverse.comment.repository;

import cluverse.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {

    boolean existsByParentId(Long parentId);
}
