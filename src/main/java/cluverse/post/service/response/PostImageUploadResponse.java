package cluverse.post.service.response;

import cluverse.post.domain.PostImageUpload;

import java.util.List;
import java.util.UUID;

public record PostImageUploadResponse(
        UUID requestId,
        String status,
        long sourceBytes,
        long outputBytes,
        double reductionPercent,
        List<PostImageAssetResponse> images
) {
    public static PostImageUploadResponse of(PostImageUpload upload) {
        long sourceBytes = upload.getTotalSourceBytes();
        long outputBytes = upload.getTotalOutputBytes();
        double reductionPercent = sourceBytes == 0
                ? 0
                : Math.max(0, (sourceBytes - outputBytes) * 100.0 / sourceBytes);
        return new PostImageUploadResponse(
                upload.getRequestId(),
                upload.getStatus().name(),
                sourceBytes,
                outputBytes,
                Math.round(reductionPercent * 100.0) / 100.0,
                upload.getAssets().stream().map(PostImageAssetResponse::of).toList()
        );
    }
}
