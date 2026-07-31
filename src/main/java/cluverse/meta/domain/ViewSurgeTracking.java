package cluverse.meta.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 조회수 급상승 추적 (조회수 증가 API V4).
 * 쓰기는 모두 네이티브 UPSERT/UPDATE — 엔티티는 조회와 테스트 스키마 생성에 쓰인다.
 */
@Entity
@Table(name = "view_surge_tracking", indexes = @Index(name = "idx_expires_at", columnList = "expires_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ViewSurgeTracking {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(nullable = false)
    private LocalDateTime activatedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private ViewSurgeTracking(Long postId, LocalDateTime activatedAt, LocalDateTime expiresAt) {
        this.postId = postId;
        this.activatedAt = activatedAt;
        this.expiresAt = expiresAt;
    }

    public static ViewSurgeTracking activate(Long postId, LocalDateTime activatedAt, LocalDateTime expiresAt) {
        return new ViewSurgeTracking(postId, activatedAt, expiresAt);
    }
}
