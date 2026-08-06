package cluverse.post.config;

import cluverse.post.properties.PostImageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PostImageProperties.class)
public class PostImageConfig {
}
