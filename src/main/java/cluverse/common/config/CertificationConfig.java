package cluverse.common.config;

import cluverse.certification.properties.CertificationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CertificationProperties.class)
public class CertificationConfig {
}
