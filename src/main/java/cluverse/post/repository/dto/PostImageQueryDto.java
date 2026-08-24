package cluverse.post.repository.dto;

public record PostImageQueryDto(
        String contentKey,
        String thumbnailKey,
        String contentUrl,
        String thumbnailUrl
) {
}
