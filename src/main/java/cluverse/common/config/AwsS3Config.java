package cluverse.common.config;

import cluverse.common.properties.AwsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class AwsS3Config {

    @Bean
    S3Client s3Client(AwsProperties awsProperties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(awsProperties.region()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (StringUtils.hasText(awsProperties.s3().endpoint())) {
            builder.endpointOverride(URI.create(awsProperties.s3().endpoint()));
            builder.forcePathStyle(true);
        }
        return builder.build();
    }

    @Bean
    @Primary
    S3Presigner s3Presigner(AwsProperties awsProperties) {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(awsProperties.region()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (StringUtils.hasText(awsProperties.s3().endpoint())) {
            builder.endpointOverride(URI.create(awsProperties.s3().endpoint()));
            builder.serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build());
        }

        return builder.build();
    }

    @Bean("publicS3Presigner")
    S3Presigner publicS3Presigner(AwsProperties awsProperties) {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(awsProperties.region()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        String publicEndpoint = awsProperties.s3().publicEndpoint();
        if (StringUtils.hasText(publicEndpoint)) {
            builder.endpointOverride(URI.create(publicEndpoint));
            builder.serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build());
        }
        return builder.build();
    }
}
