package cluverse.meta.properties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "view-count")
@Validated
public record ViewCountProperties(
        @DefaultValue("30m") @NotNull Duration duplicateTtl,
        @DefaultValue("1m") @NotNull Duration deltaFlushInterval,
        @DefaultValue("100") @Positive long threshold,
        @DefaultValue("1m") @NotNull Duration checkpointInterval,
        @DefaultValue("30m") @NotNull Duration inactiveAfter,
        @DefaultValue("1000") @Positive int scanCount,
        @DefaultValue("1000") @Positive int batchSize,
        @DefaultValue("1s") @NotNull Duration initializationLockLease,
        @DefaultValue("10ms") @NotNull Duration initializationWait,
        @DefaultValue("5") @Positive int initializationAttempts
) {
}
