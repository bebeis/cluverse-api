package cluverse.post.service.implement;

import cluverse.post.domain.PostImageUpload;

public sealed interface PostImageUploadReservationResult {

    PostImageUpload upload();

    record Created(PostImageUpload upload) implements PostImageUploadReservationResult {
    }

    record Existing(PostImageUpload upload) implements PostImageUploadReservationResult {
    }
}
