package cluverse.home.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "home.recent-commented-posts")
public record HomeRecentCommentProperties(
        @DefaultValue("1m") @NotNull Duration snapshotCacheTtl,
        @DefaultValue("200") @Min(10) int snapshotCandidateSize
) {
}
