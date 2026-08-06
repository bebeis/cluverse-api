package cluverse.common.config;

import cluverse.popularity.properties.PopularityInlineEvaluationProperties;
import cluverse.popularity.properties.PopularityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        PopularityProperties.class,
        PopularityInlineEvaluationProperties.class
})
public class PopularityConfig {
}
