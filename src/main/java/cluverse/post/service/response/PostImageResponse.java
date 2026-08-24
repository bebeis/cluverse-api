package cluverse.post.service.response;

import cluverse.post.repository.dto.PostImageQueryDto;

public record PostImageResponse(
        String contentKey,
        String thumbnailKey,
        String contentUrl,
        String thumbnailUrl
) {
    public static PostImageResponse from(PostImageQueryDto image) {
        return new PostImageResponse(
                image.contentKey(),
                image.thumbnailKey(),
                image.contentUrl(),
                image.thumbnailUrl()
        );
    }
}
