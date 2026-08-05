package cluverse.post.service.implement;

import cluverse.post.domain.PostImageUpload;

public record PostImageUploadReservationResult(
        PostImageUpload upload,
        boolean created
) {
}
