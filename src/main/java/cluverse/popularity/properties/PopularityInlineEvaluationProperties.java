package cluverse.popularity.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "popularity.inline-evaluation")
public record PopularityInlineEvaluationProperties(
        @DefaultValue("true") boolean enabled
) {
}
