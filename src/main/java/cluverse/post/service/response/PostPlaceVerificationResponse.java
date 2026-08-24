package cluverse.post.service.response;

import cluverse.post.domain.PostPlaceVerification;

public record PostPlaceVerificationResponse(
        String status
) {
    public static PostPlaceVerificationResponse from(PostPlaceVerification verification) {
        return new PostPlaceVerificationResponse(
                verification == null ? "NONE" : verification.getStatus().name()
        );
    }
}
