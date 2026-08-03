package cluverse.comment.repository;

import cluverse.comment.domain.Comment;
import cluverse.comment.domain.CommentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {

    boolean existsByPostIdAndParentId(Long postId, Long parentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select comment from Comment comment where comment.id = :commentId")
    Optional<Comment> findByIdForUpdate(@Param("commentId") Long commentId);

    Optional<Comment> findByMemberIdAndClientRequestId(Long memberId, String clientRequestId);

    Optional<Comment> findFirstByPostIdAndStatusNotOrderByCreatedAtDescIdDesc(
            Long postId,
            CommentStatus status
    );
}
