package cluverse.popularity.repository.dto;

public record PopularityPolicySample(
        Long scoreAtPromotion,
        long likeCount,
        long commentCount,
        long viewCount
) {
}
