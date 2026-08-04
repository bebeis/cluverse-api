package cluverse.comment.repository;

import cluverse.comment.domain.PostCommentActivity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostCommentActivityRepository
        extends JpaRepository<PostCommentActivity, Long>, PostCommentActivityRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select activity from PostCommentActivity activity where activity.postId = :postId")
    Optional<PostCommentActivity> findByPostIdForUpdate(@Param("postId") Long postId);
}
