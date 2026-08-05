package cluverse.meta.repository.dto;

public record ResidentViewCount(Long postId, long viewCount, long lastCountedAtMillis) {

    public ViewCountSnapshot toSnapshot() {
        return new ViewCountSnapshot(postId, viewCount);
    }
}
