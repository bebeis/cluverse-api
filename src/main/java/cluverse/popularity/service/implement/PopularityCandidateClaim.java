package cluverse.popularity.service.implement;

import java.time.LocalDateTime;

public record PopularityCandidateClaim(Long postId, LocalDateTime dueAt) {
}
