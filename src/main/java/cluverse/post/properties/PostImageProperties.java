package cluverse.post.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "post-image")
public record PostImageProperties(
        @DefaultValue("10MB") DataSize maxFileSize
) {
}
