package cluverse.meta.repository;

import cluverse.meta.domain.ViewSurgeTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ViewSurgeTrackingRepository extends JpaRepository<ViewSurgeTracking, Long> {

    // VALUES(expires_at) 대신 파라미터 재바인딩 — VALUES()는 MySQL 8.0.20+ deprecated, H2 미지원
    @Modifying
    @Query(value = """
            INSERT INTO view_surge_tracking (post_id, activated_at, expires_at)
            VALUES (:postId, :activatedAt, :expiresAt)
            ON DUPLICATE KEY UPDATE expires_at = GREATEST(expires_at, :expiresAt)
            """, nativeQuery = true)
    int upsertActivation(
            @Param("postId") Long postId,
            @Param("activatedAt") LocalDateTime activatedAt,
            @Param("expiresAt") LocalDateTime expiresAt
    );

    @Modifying
    @Query(value = """
            UPDATE view_surge_tracking
            SET expires_at = :extendedExpiresAt
            WHERE post_id IN (:postIds)
            """, nativeQuery = true)
    int extendExpiryAll(@Param("postIds") List<Long> postIds, @Param("extendedExpiresAt") LocalDateTime extendedExpiresAt);

    // 만료가 늦은(=가장 최근까지 뜨거운) 글부터 — LIMIT 이 라우팅 캐시 상한을 강제한다
    @Query(value = """
            SELECT post_id
            FROM view_surge_tracking
            WHERE expires_at > :now
            ORDER BY expires_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findActivePostIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    // expires_at 정렬이라야 idx_expires_at 범위 스캔이 LIMIT만큼만 읽는다
    @Query(value = """
            SELECT post_id
            FROM view_surge_tracking
            WHERE expires_at <= :cutoff
            ORDER BY expires_at
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findExpiredPostIds(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);
}
