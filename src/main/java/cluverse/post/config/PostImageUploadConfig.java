package cluverse.post.config;

import cluverse.common.properties.AwsProperties;
import cluverse.post.properties.PostImageUploadProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Configuration
@EnableConfigurationProperties(PostImageUploadProperties.class)
public class PostImageUploadConfig {

    @Bean
    LambdaClient postImageLambdaClient(
            AwsProperties awsProperties,
            PostImageUploadProperties properties
    ) {
        LambdaClientBuilder builder = LambdaClient.builder()
                .region(Region.of(awsProperties.region()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (StringUtils.hasText(properties.lambdaEndpoint())) {
            builder.endpointOverride(URI.create(properties.lambdaEndpoint()));
        }
        return builder.build();
    }

    @Bean(name = "postImageVirtualExecutor", destroyMethod = "shutdown")
    ExecutorService postImageVirtualExecutor() {
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("image-upload-virtual-", 0).factory()
        );
    }

    @Bean
    Semaphore postImageRemoteCallSemaphore(PostImageUploadProperties properties) {
        return new Semaphore(properties.maxConcurrentRemoteCalls(), true);
    }

}
