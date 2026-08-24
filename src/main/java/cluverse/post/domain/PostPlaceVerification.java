package cluverse.post.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostPlaceVerification extends BaseTimeEntity {

    @Id
    private Long postId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PostPlaceVerificationStatus status;

    @Column(length = 500)
    private String failureReason;

    private PostPlaceVerification(Long postId) {
        this.postId = postId;
        this.status = PostPlaceVerificationStatus.PENDING;
    }

    public static PostPlaceVerification pending(Long postId) {
        return new PostPlaceVerification(postId);
    }

    public void complete() {
        status = PostPlaceVerificationStatus.COMPLETED;
        failureReason = null;
    }

    public void fail(String reason) {
        status = PostPlaceVerificationStatus.FAILED;
        failureReason = reason == null ? "unknown"
                : reason.substring(0, Math.min(reason.length(), 500));
    }
}
