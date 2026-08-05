package cluverse.post.repository;

import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.domain.PostImageUpload;
import cluverse.post.domain.PostImageUploadStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostImageUploadRepository extends JpaRepository<PostImageUpload, Long> {

    @EntityGraph(attributePaths = "assets")
    Optional<PostImageUpload> findByRequestIdAndVersion(UUID requestId, ImageUploadVersion version);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT upload FROM PostImageUpload upload WHERE upload.id = :uploadId")
    Optional<PostImageUpload> findByIdForUpdate(@Param("uploadId") Long uploadId);

    @EntityGraph(attributePaths = "assets")
    List<PostImageUpload> findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
            PostImageUploadStatus status,
            LocalDateTime updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PostImageUpload upload
            SET upload.status = :claimedStatus, upload.updatedAt = CURRENT_TIMESTAMP
            WHERE upload.id = :uploadId
              AND upload.status = :pendingStatus
              AND upload.updatedAt < :threshold
            """)
    int claimStalePending(
            @Param("uploadId") Long uploadId,
            @Param("threshold") LocalDateTime threshold,
            @Param("pendingStatus") PostImageUploadStatus pendingStatus,
            @Param("claimedStatus") PostImageUploadStatus claimedStatus
    );

    @EntityGraph(attributePaths = "assets")
    List<PostImageUpload> findTop100ByStatusAndStagingCleanedFalseOrderByUpdatedAtAsc(
            PostImageUploadStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PostImageUpload upload SET upload.updatedAt = CURRENT_TIMESTAMP WHERE upload.id = :uploadId")
    int touchUpdatedAt(@Param("uploadId") Long uploadId);

    long countByStatus(PostImageUploadStatus status);
}
