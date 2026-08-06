package cluverse.common.config;

import cluverse.post.properties.PostListCacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PostListCacheProperties.class)
public class PostListCacheConfig {
}
