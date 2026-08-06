package cluverse.post.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "post-list-cache")
public record PostListCacheProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("201") @Min(2) int maxEntries,
        @DefaultValue("3m") @NotNull Duration ttl,
        @DefaultValue("2s") @NotNull Duration warmupLockTtl
) {
    public PostListCacheProperties {
        if (ttl != null && ttl.toSeconds() < 1) {
            throw new IllegalArgumentException("ttl은 1초 이상이어야 합니다.");
        }
        if (warmupLockTtl != null && (warmupLockTtl.isZero() || warmupLockTtl.isNegative())) {
            throw new IllegalArgumentException("warmupLockTtl은 0보다 커야 합니다.");
        }
    }
}
