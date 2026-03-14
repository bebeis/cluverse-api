package cluverse.post.client;

public record PresignedUploadResult(
        String uploadUrl,
        String imageUrl
) {
}
