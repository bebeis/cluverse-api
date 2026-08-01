package cluverse.common.config;

import cluverse.place.properties.LocalMapProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LocalMapProperties.class)
public class LocalMapConfig {
}
