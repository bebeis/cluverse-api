package cluverse.post.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@ConfigurationProperties(prefix = "post-image-upload")
public record PostImageUploadProperties(
        String lambdaFunctionName,
        String lambdaEndpoint,
        @DefaultValue("10MB") DataSize maxFileSize,
        @DefaultValue("16") int maxConcurrentRemoteCalls,
        @DefaultValue("30s") Duration remoteTimeout,
        @DefaultValue("3m") Duration stalePendingAfter,
        @DefaultValue("30s") Duration cleanupInterval
) {
}
