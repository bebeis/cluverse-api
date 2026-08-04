package cluverse.common.config;

import cluverse.home.properties.HomeRecentCommentProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HomeRecentCommentProperties.class)
public class HomeConfig {
}
