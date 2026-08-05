package cluverse.post.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@ConfigurationProperties(prefix = "image-upload-experiment")
public record PostImageUploadProperties(
        @DefaultValue("false") boolean enabled,
        String benchmarkToken,
        String lambdaFunctionName,
        String lambdaEndpoint,
        @DefaultValue("10MB") DataSize maxFileSize,
        @DefaultValue("32") int platformQueueCapacity,
        @DefaultValue("16") int maxConcurrentRemoteCalls,
        @DefaultValue("30s") Duration remoteTimeout,
        @DefaultValue("3m") Duration stalePendingAfter,
        @DefaultValue("30s") Duration cleanupInterval
) {
}
