package cluverse.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

@Configuration
@EnableResilientMethods(proxyTargetClass = true)
public class RetryConfig {
}
